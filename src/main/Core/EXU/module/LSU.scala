package Core.EXU.module

import chisel3._
import chisel3.util._

import Core.Config.Config
import Core.IDU.module._
import Core.Bus._

object LSUCtrl {
  object len {
    val WIDTH = log2Ceil(4)
    val NOP   = 0.U(WIDTH.W)
    val B     = 1.U(WIDTH.W)
    val H     = 2.U(WIDTH.W)
    val W     = 3.U(WIDTH.W)
  }
  val defaultList = List(false.B, len.NOP)
  val table = Array(
    BitPat(mem.NOP)     -> List(false.B, len.NOP),
    BitPat(mem.LB)      -> List(true.B, len.B),
    BitPat(mem.LH)      -> List(true.B, len.H),
    BitPat(mem.LBU)     -> List(false.B, len.B),
    BitPat(mem.LHU)     -> List(false.B, len.H),
    BitPat(mem.LW)      -> List(true.B, len.W),
    BitPat(mem.SB)      -> List(false.B, len.B),
    BitPat(mem.SH)      -> List(false.B, len.H),
    BitPat(mem.SW)      -> List(false.B, len.W),
    BitPat(mem.AMOSWAP) -> List(false.B, len.W),
    BitPat(mem.AMOADD)  -> List(false.B, len.W),
    BitPat(mem.AMOXOR)  -> List(false.B, len.W),
    BitPat(mem.AMOAND)  -> List(false.B, len.W),
    BitPat(mem.AMOOR)   -> List(false.B, len.W),
    BitPat(mem.AMOMIN)  -> List(false.B, len.W),
    BitPat(mem.AMOMAX)  -> List(false.B, len.W),
    BitPat(mem.AMOMINU) -> List(false.B, len.W),
    BitPat(mem.AMOMAXU) -> List(false.B, len.W),
    BitPat(mem.LR)      -> List(false.B, len.W),
    BitPat(mem.SC)      -> List(false.B, len.W)
  )
}

class LSUBundle extends Bundle {
  val memCtrl = Input(mem())                     // LSU 控制信号
  val addr    = Input(UInt(Config.Addr.Width.W)) // 地址
  val dataIn  = Input(UInt(Config.Data.XLEN.W))  // 存数/AMO 操作数
  val flush   = Input(Bool())                    // 冲刷
  val dataOut = Output(UInt(Config.Data.XLEN.W)) // 结果
  val ready   = Output(Bool())                   // 完成
  // 本次访存的提交信息（纯旁路观测；Sim.Enable=false 时为零宽）
  val obsAccess  = new Bundle {
    val valid = Output(Config.Sim.obsUInt(1))
    val write = Output(Config.Sim.obsUInt(1))
    val addr  = Output(Config.Sim.obsUInt(Config.Addr.Width))
    val wdata = Output(Config.Sim.obsUInt(Config.Data.XLEN))
    val size  = Output(Config.Sim.obsUInt(2)) // MemSize.B/H/W
  }
}

class LSU extends Module {
  val ioLSU     = IO(new LSUBundle)
  val ioMemPort = IO(new MemPort) // 数据存储器（主侧，统一访存接口）

  val XLEN = Config.Data.XLEN
  val AW   = Config.Addr.Width

  val isSigned :: dataLen :: Nil = ListLookup(ioLSU.memCtrl.asUInt, LSUCtrl.defaultList, LSUCtrl.table)

  val sIdle :: sRead :: sWrite :: sDone :: Nil = Enum(4)
  val state                                    = RegInit(sIdle)

  val ctrlReg = RegInit(mem.NOP)
  val addrReg = RegInit(0.U(AW.W))
  val dataReg = RegInit(0.U(XLEN.W))
  val oldReg  = RegInit(0.U(XLEN.W))
  val lenReg  = RegInit(0.U(LSUCtrl.len.WIDTH.W))
  val signReg = RegInit(false.B)

  val start = (state === sIdle) && (ioLSU.memCtrl =/= mem.NOP)

  val curCtrl = Mux(state === sIdle, ioLSU.memCtrl, ctrlReg)
  val curAddr = Mux(state === sIdle, ioLSU.addr, addrReg)
  val curLen  = Mux(state === sIdle, dataLen, lenReg)
  val curSign = Mux(state === sIdle, isSigned.asBool, signReg)

  val isLoad  = (curCtrl === mem.LB) || (curCtrl === mem.LH) || (curCtrl === mem.LBU) || (curCtrl === mem.LHU) || (curCtrl === mem.LW)
  val isStore = (curCtrl === mem.SB) || (curCtrl === mem.SH) || (curCtrl === mem.SW)
  val isAMO   = (curCtrl === mem.AMOSWAP) || (curCtrl === mem.AMOADD) || (curCtrl === mem.AMOXOR) || (curCtrl === mem.AMOAND) ||
    (curCtrl === mem.AMOOR) || (curCtrl === mem.AMOMIN) || (curCtrl === mem.AMOMAX) ||
    (curCtrl === mem.AMOMINU) || (curCtrl === mem.AMOMAXU)
  val isLR    = curCtrl === mem.LR
  val isSC    = curCtrl === mem.SC

  // 访存提交信息（纯旁路观测；Sim.Enable=false 时全部零宽）
  val curSize = Mux(curLen === LSUCtrl.len.B, MemSize.B, Mux(curLen === LSUCtrl.len.H, MemSize.H, MemSize.W))
  if (Config.Sim.Enable) {
    ioLSU.obsAccess.valid := (state === sDone) && (ctrlReg =/= mem.NOP)
    ioLSU.obsAccess.write := isStore || isAMO || isSC
    ioLSU.obsAccess.addr  := addrReg
    ioLSU.obsAccess.wdata := dataReg
    ioLSU.obsAccess.size  := curSize
  } else {
    ioLSU.obsAccess.valid := DontCare
    ioLSU.obsAccess.write := DontCare
    ioLSU.obsAccess.addr  := DontCare
    ioLSU.obsAccess.wdata := DontCare
    ioLSU.obsAccess.size  := DontCare
  }

  // 原子操作编码（下沉到缓存）
  val amoOp = Mux(
    curCtrl === mem.LR,
    AmoOp.LR,
    Mux(
      curCtrl === mem.SC,
      AmoOp.SC,
      MuxCase(
        AmoOp.NOP,
        Seq(
          (curCtrl === mem.AMOSWAP) -> AmoOp.SWAP,
          (curCtrl === mem.AMOADD)  -> AmoOp.ADD,
          (curCtrl === mem.AMOXOR)  -> AmoOp.XOR,
          (curCtrl === mem.AMOAND)  -> AmoOp.AND,
          (curCtrl === mem.AMOOR)   -> AmoOp.OR,
          (curCtrl === mem.AMOMIN)  -> AmoOp.MIN,
          (curCtrl === mem.AMOMAX)  -> AmoOp.MAX,
          (curCtrl === mem.AMOMINU) -> AmoOp.MINU,
          (curCtrl === mem.AMOMAXU) -> AmoOp.MAXU
        )
      )
    )
  )

  // MemPort 请求映射
  val memSize  = Mux(curLen === LSUCtrl.len.B, MemSize.B, Mux(curLen === LSUCtrl.len.H, MemSize.H, MemSize.W))
  val memWstrb = Mux(curLen === LSUCtrl.len.B, "b0001".U(4.W), Mux(curLen === LSUCtrl.len.H, "b0011".U(4.W), "b1111".U(4.W)))

  // 单 outstanding 内存事务
  val memBusy  = RegInit(false.B)
  val needMem  = (state === sRead) || (state === sWrite)
  val issueMem = needMem && !memBusy

  ioMemPort.req.valid      := issueMem
  ioMemPort.req.bits.addr  := addrReg
  ioMemPort.req.bits.write := state === sWrite
  ioMemPort.req.bits.size  := memSize
  ioMemPort.req.bits.wdata := dataReg
  ioMemPort.req.bits.wstrb := memWstrb
  ioMemPort.req.bits.amoOp := amoOp
  when(ioMemPort.req.fire) { memBusy := true.B }
  ioMemPort.resp.ready     := memBusy

  // load 符号/零扩展
  val byteSE   = Cat(Fill(24, oldReg(7)), oldReg(7, 0))
  val halfSE   = Cat(Fill(16, oldReg(15)), oldReg(15, 0))
  val byteZE   = Cat(0.U(24.W), oldReg(7, 0))
  val halfZE   = Cat(0.U(16.W), oldReg(15, 0))
  val isB      = curLen === LSUCtrl.len.B
  val isH      = curLen === LSUCtrl.len.H
  val loadData = Mux(curSign, Mux(isB, byteSE, Mux(isH, halfSE, oldReg)), Mux(isB, byteZE, Mux(isH, halfZE, oldReg)))
  val isLoadOp = isLoad

  ioLSU.dataOut := Mux(isLoadOp, loadData, oldReg)
  ioLSU.ready   := (state === sIdle && ioLSU.memCtrl === mem.NOP) || (state === sDone)

  when(ioLSU.flush) {
    state   := sIdle
    memBusy := false.B
  }.elsewhen(start) {
    ctrlReg := ioLSU.memCtrl
    addrReg := ioLSU.addr
    dataReg := ioLSU.dataIn
    lenReg  := dataLen
    signReg := isSigned.asBool
    when(isLoad || isLR) { state := sRead }
      .elsewhen(isStore || isSC || isAMO) { state := sWrite }
  }.elsewhen(ioMemPort.resp.fire) {
    memBusy := false.B
    oldReg  := ioMemPort.resp.bits.rdata(Config.Data.XLEN - 1, 0)
    state   := sDone
  }.elsewhen(state === sDone) {
    state := sIdle
  }
}
