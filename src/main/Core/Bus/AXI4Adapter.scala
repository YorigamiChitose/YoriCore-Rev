package Core.Bus

import chisel3._
import chisel3.util._

import Core.Config.Config

// 统一 AXI4 适配器：唯一「理解 AXI」的模块。
// 接收统一 MemPort（256 位），按 size 执行：
//   - size=行   ：行对齐地址，len=(LineBits/AxiDataWidth)-1 的 64 位突发（读填行 / 写回行）
//   - size=字/半字/字节：len=0 单拍窄传输（不可缓存/MMIO 直通）
class AXI4Adapter extends Module {
  val io = IO(new Bundle {
    val mem = Flipped(new MemPort)
    val axi = new AXI4Master(Config.Bus.AxiDataWidth)
  })

  val axiDataWidth = Config.Bus.AxiDataWidth // 64
  val lineBits     = Config.Cache.LineBits   // 256
  val lineBeats    = lineBits / axiDataWidth // 4
  val beatBytes    = axiDataWidth / 8        // 8

  val sIdle :: sAw :: sW :: sB :: sAr :: sR :: sResp :: Nil = Enum(7)
  val state = RegInit(sIdle)

  val rAddr  = RegInit(0.U(Config.Addr.Width.W))
  val rWrite = RegInit(false.B)
  val rSize  = RegInit(0.U(2.W))
  val rData  = RegInit(0.U(lineBits.W))
  val rStrb  = RegInit(0.U((lineBits / 8).W))
  val rAmo   = RegInit(0.U(4.W))
  val rAcc   = RegInit(0.U(lineBits.W))

  val isLine = rSize === MemSize.Line

  // 行对齐地址（低 LineWidth 位置 0）
  val lineAddr = Cat(rAddr(Config.Addr.Width - 1, Config.Cache.LineWidth), 0.U(Config.Cache.LineWidth.W))

  val beats   = Mux(isLine, lineBeats.U, 1.U)
  val beatCnt = RegInit(0.U(log2Ceil(lineBeats).W))

  // 窄传输在 64 位总线内的字节偏移（0..7）
  val byteOff = rAddr(2, 0)
  val wData64 = Cat(0.U(32.W), rData(31, 0)) << (byteOff * 8.U)
  val wStrb64 = Cat(0.U(4.W), rStrb(3, 0)) << byteOff

  // 行写：按 beat 取 64 位片
  val wBeatData = (rData >> (beatCnt * axiDataWidth.U))(axiDataWidth - 1, 0)

  val len = Mux(isLine, (lineBeats - 1).U, 0.U)

  // —— AXI 默认输出 ——
  io.axi.aw.bits.id    := 0.U
  io.axi.aw.bits.len   := len
  io.axi.aw.bits.size  := 3.U(3.W) // 8 字节/拍
  io.axi.aw.bits.burst := AXI4.Burst.INCR
  io.axi.aw.bits.cache := 0.U
  io.axi.aw.bits.prot  := 0.U

  io.axi.ar.bits.id    := 0.U
  io.axi.ar.bits.len   := len
  io.axi.ar.bits.size  := 3.U(3.W)
  io.axi.ar.bits.burst := AXI4.Burst.INCR
  io.axi.ar.bits.cache := 0.U
  io.axi.ar.bits.prot  := 0.U

  io.mem.req.ready      := state === sIdle
  io.mem.resp.valid     := state === sResp
  io.mem.resp.bits.rdata := rAcc
  // 不可缓存地址上的原子操作（amoOp=/=NOP）不受支持，到达边界即报总线错误（方案 1）
  io.mem.resp.bits.err  := rAmo =/= AmoOp.NOP

  // 请求锁存
  when(state === sIdle && io.mem.req.valid) {
    rAddr   := io.mem.req.bits.addr
    rWrite  := io.mem.req.bits.write
    rSize   := io.mem.req.bits.size
    rData   := io.mem.req.bits.wdata
    rStrb   := io.mem.req.bits.wstrb
    rAmo    := io.mem.req.bits.amoOp
    beatCnt := 0.U
    rAcc    := 0.U
    when(io.mem.req.bits.write) { state := sAw }
    .otherwise { state := sAr }
  }

  // —— 写通道 ——
  io.axi.aw.valid := state === sAw
  io.axi.aw.bits.addr := Mux(isLine, lineAddr, rAddr)
  when(state === sAw && io.axi.aw.ready) { state := sW }

  io.axi.w.valid      := state === sW
  io.axi.w.bits.data  := Mux(isLine, wBeatData, wData64)
  io.axi.w.bits.strb  := Mux(isLine, Fill(beatBytes, 1.U), wStrb64)
  io.axi.w.bits.last  := beatCnt === (beats - 1.U)
  when(state === sW && io.axi.w.ready) {
    when(beatCnt === (beats - 1.U)) { state := sB }
    .otherwise { beatCnt := beatCnt + 1.U }
  }

  io.axi.b.ready := state === sB
  when(state === sB && io.axi.b.valid) { state := sResp }

  // —— 读通道 ——
  io.axi.ar.valid := state === sAr
  io.axi.ar.bits.addr := Mux(isLine, lineAddr, rAddr)
  when(state === sAr && io.axi.ar.ready) { state := sR }

  io.axi.r.ready := state === sR
  when(state === sR && io.axi.r.valid) {
    when(isLine) {
      rAcc := rAcc | (io.axi.r.bits.data << (beatCnt * axiDataWidth.U))
    }.otherwise {
      val wd = (io.axi.r.bits.data >> (byteOff * 8.U))(31, 0)
      rAcc := Cat(0.U((lineBits - 32).W), wd)
    }
    when(beatCnt === (beats - 1.U)) { state := sResp }
    .otherwise { beatCnt := beatCnt + 1.U }
  }

  when(state === sResp && io.mem.resp.ready) { state := sIdle }
}