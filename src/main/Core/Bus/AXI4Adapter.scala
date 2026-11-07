package Core.Bus

import chisel3._
import chisel3.util._
import Core.Config.Config

// AXI4 Adapter —— 核内自定义 req/resp（MemPort）到对外 AXI4 主端口的唯一转换点。
// Phase 0：单拍（无 burst）、单 outstanding、32-bit 数据宽度。
// 后续 Phase 2 才在本模块内加入 burst / cacheable / 64-bit。
class AXI4Adapter extends Module {
  val io = IO(new Bundle {
    val mem = Flipped(new MemPort) // 自定义接口（从侧）
    val axi = new AXI4Master        // AXI4 主端口
  })

  val AW = Config.Bus.AxiAddrWidth
  val DW = Config.Bus.AxiDataWidth
  val SW = DW / 8

  val sIdle :: sReadAR :: sReadR :: sWrite :: sWriteB :: sResp :: Nil = Enum(6)
  val state = RegInit(sIdle)

  // 单 outstanding，锁存本次请求
  val reqAddr = RegInit(0.U(AW.W))
  val reqSize = RegInit(0.U(2.W))
  val reqData = RegInit(0.U(DW.W))

  io.mem.req.ready := state === sIdle
  val reqFire = io.mem.req.valid && io.mem.req.ready
  when(reqFire) {
    reqAddr := io.mem.req.bits.addr
    reqSize := io.mem.req.bits.size
    reqData := io.mem.req.bits.wdata
  }

  val isByte = reqSize === 0.U
  val isHalf = reqSize === 1.U
  val axiSize = Mux(isByte, 0.U(3.W), Mux(isHalf, 1.U(3.W), 2.U(3.W)))

  val byteOff = reqAddr(1, 0)
  val halfOff = reqAddr(1)

  // 写：把右对齐 data/strb 摆到对应字节 lane（小端）
  val wdataByte = MuxLookup(byteOff, 0.U(DW.W))(Seq(
    0.U -> Cat(Fill(24, 0.U), reqData(7, 0)),
    1.U -> Cat(Fill(16, 0.U), reqData(7, 0), 0.U(8.W)),
    2.U -> Cat(Fill(8, 0.U), reqData(7, 0), 0.U(16.W)),
    3.U -> Cat(reqData(7, 0), 0.U(24.W))
  ))
  val wdataHalf = Mux(halfOff, Cat(reqData(15, 0), 0.U(16.W)), Cat(Fill(16, 0.U), reqData(15, 0)))
  val wdataOut  = Mux(isByte, wdataByte, Mux(isHalf, wdataHalf, reqData))

  val strbByte = MuxLookup(byteOff, 0.U(SW.W))(Seq(
    0.U -> "b0001".U,
    1.U -> "b0010".U,
    2.U -> "b0100".U,
    3.U -> "b1000".U
  ))
  val strbHalf = Mux(halfOff, "b1100".U(SW.W), "b0011".U(SW.W))
  val wstrbOut = Mux(isByte, strbByte, Mux(isHalf, strbHalf, Fill(SW, 1.U)))

  // 读：从字中提取字节/半字，右对齐返回（符号扩展由 LSU 层负责）
  val rdataByte = MuxLookup(byteOff, 0.U(DW.W))(Seq(
    0.U -> Cat(Fill(24, 0.U), io.axi.r.bits.data(7, 0)),
    1.U -> Cat(Fill(24, 0.U), io.axi.r.bits.data(15, 8)),
    2.U -> Cat(Fill(24, 0.U), io.axi.r.bits.data(23, 16)),
    3.U -> Cat(Fill(24, 0.U), io.axi.r.bits.data(31, 24))
  ))
  val rdataHalf = Mux(halfOff,
    Cat(Fill(16, 0.U), io.axi.r.bits.data(31, 16)),
    Cat(Fill(16, 0.U), io.axi.r.bits.data(15, 0)))
  val rdataOut = Mux(isByte, rdataByte, Mux(isHalf, rdataHalf, io.axi.r.bits.data))

  // 响应锁存
  val respData = RegInit(0.U(DW.W))
  val respErr  = RegInit(false.B)

  // ---- AXI 默认输出 ----
  io.axi.ar.valid := false.B
  io.axi.ar.bits.id := 0.U
  io.axi.ar.bits.addr := 0.U(AW.W)
  io.axi.ar.bits.len := 0.U
  io.axi.ar.bits.size := 0.U
  io.axi.ar.bits.burst := AXI4.Burst.INCR
  io.axi.ar.bits.cache := 0.U
  io.axi.ar.bits.prot := 0.U

  io.axi.aw.valid := false.B
  io.axi.aw.bits.id := 0.U
  io.axi.aw.bits.addr := 0.U(AW.W)
  io.axi.aw.bits.len := 0.U
  io.axi.aw.bits.size := 0.U
  io.axi.aw.bits.burst := AXI4.Burst.INCR
  io.axi.aw.bits.cache := 0.U
  io.axi.aw.bits.prot := 0.U

  io.axi.w.valid := false.B
  io.axi.w.bits.data := 0.U(DW.W)
  io.axi.w.bits.strb := 0.U(SW.W)
  io.axi.w.bits.last := false.B

  io.axi.b.ready := false.B
  io.axi.r.ready := false.B

  // ---- 响应输出（仅在 sResp 拉高）----
  io.mem.resp.valid := state === sResp
  io.mem.resp.bits.rdata := respData
  io.mem.resp.bits.err   := respErr

  // ---- 状态机 ----
  switch(state) {
    is(sIdle) {
      when(reqFire) {
        state := Mux(io.mem.req.bits.write, sWrite, sReadAR)
      }
    }
    is(sReadAR) {
      io.axi.ar.valid := true.B
      io.axi.ar.bits.addr := reqAddr
      io.axi.ar.bits.size := axiSize
      when(io.axi.ar.ready) { state := sReadR }
    }
    is(sReadR) {
      io.axi.r.ready := true.B
      when(io.axi.r.valid) {
        respData := rdataOut
        respErr  := io.axi.r.bits.resp =/= AXI4.Resp.OKAY
        state := sResp
      }
    }
    is(sWrite) {
      io.axi.aw.valid := true.B
      io.axi.aw.bits.addr := reqAddr
      io.axi.aw.bits.size := axiSize
      io.axi.w.valid := true.B
      io.axi.w.bits.data := wdataOut
      io.axi.w.bits.strb := wstrbOut
      io.axi.w.bits.last := true.B
      when(io.axi.aw.ready && io.axi.w.ready) { state := sWriteB }
    }
    is(sWriteB) {
      io.axi.b.ready := true.B
      when(io.axi.b.valid) {
        respData := 0.U(DW.W)
        respErr  := io.axi.b.bits.resp =/= AXI4.Resp.OKAY
        state := sResp
      }
    }
    is(sResp) {
      when(io.mem.resp.ready) { state := sIdle }
    }
  }
}