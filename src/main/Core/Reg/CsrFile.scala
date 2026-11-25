package Core.Reg

import chisel3._
import chisel3.util._

import Core.Config.Config
import Core.Reg.module._
import Core.IDU.module._
import Core.Device.CoreIrq

// TODO: 完善M级 完成S级 U级

class BundleMEPC extends Bundle {
  val pc      = UInt(Config.Csr.DataWidth.W)
  def initVal = 0.U.asTypeOf(this)
}

class BundleMCAUSE extends Bundle {
  val causeId = UInt(Config.Csr.DataWidth.W)
  def initVal = 0.U.asTypeOf(this)
}

class BundleMSTATUS extends Bundle {
  val SD          = UInt(1.W)
  val XLEN_SUB_24 = UInt((Config.Csr.DataWidth - 24).W)
  val TSR         = UInt(1.W)
  val TW          = UInt(1.W)
  val TVM         = UInt(1.W)
  val MXR         = UInt(1.W)
  val SUM         = UInt(1.W)
  val MPRV        = UInt(1.W)
  val XS          = UInt(2.W)
  val FS          = UInt(2.W)
  val MPP         = UInt(2.W)
  val ZERO_2      = UInt(2.W)
  val SPP         = UInt(1.W)
  val MPIE        = UInt(1.W)
  val ZERO_1      = UInt(1.W)
  val SPIE        = UInt(1.W)
  val UPIE        = UInt(1.W)
  val MIE         = UInt(1.W)
  val ZERO_0      = UInt(1.W)
  val SIE         = UInt(1.W)
  val UIE         = UInt(1.W)
  def initVal     = "h1800".U.asTypeOf(this)
}

class BundleMTVEC extends Bundle {
  val causeId = UInt(Config.Csr.DataWidth.W)
  def initVal = 0.U.asTypeOf(this)
}

object CSR_MAP {
  val MEPC    = "h341".U(Config.Csr.AddrWidth.W)
  val MCAUSE  = "h342".U(Config.Csr.AddrWidth.W)
  val MSTATUS = "h300".U(Config.Csr.AddrWidth.W)
  val MIE     = "h304".U(Config.Csr.AddrWidth.W)
  val MTVEC   = "h305".U(Config.Csr.AddrWidth.W)
  val MIP     = "h344".U(Config.Csr.AddrWidth.W)
  val SSTATUS = "h100".U(Config.Csr.AddrWidth.W)
  val SIE     = "h104".U(Config.Csr.AddrWidth.W)
  val STVEC   = "h105".U(Config.Csr.AddrWidth.W)
  val SSCRATCH= "h140".U(Config.Csr.AddrWidth.W)
  val SEPC    = "h141".U(Config.Csr.AddrWidth.W)
  val SCAUSE  = "h142".U(Config.Csr.AddrWidth.W)
  val STVAL   = "h143".U(Config.Csr.AddrWidth.W)
  val SIP     = "h144".U(Config.Csr.AddrWidth.W)
  val SATP    = "h180".U(Config.Csr.AddrWidth.W)
  val MEDELEG = "h302".U(Config.Csr.AddrWidth.W)
  val MIDELEG = "h303".U(Config.Csr.AddrWidth.W)
}

class CsrFile extends Module {
  val ioCSRIDU  = IO(new CSRIDUBundle())
  val ioCSRWBU  = IO(new CSRWBUBundle())
  val ioIrq     = IO(Input(new CoreIrq))
  val ioIrqPending   = IO(Output(Bool()))
  val ioIrqCause     = IO(Output(UInt(Config.Csr.DataWidth.W)))
  val ioIrqTrapPC    = IO(Output(UInt(Config.Csr.DataWidth.W)))
  val ioIrqTake      = IO(Input(Bool()))
  val ioIrqMepc      = IO(Input(UInt(Config.Csr.DataWidth.W)))
  val ioIrqCauseIn   = IO(Input(UInt(Config.Csr.DataWidth.W)))
  // 仿真观测输出：Sim.Enable=false 时全部不存在
  val ioPriv    = if (Config.Sim.Enable) Some(IO(Output(UInt(2.W)))) else None
  val ioMepc    = if (Config.Sim.Enable) Some(IO(Output(UInt(Config.Csr.DataWidth.W)))) else None
  val ioMcause  = if (Config.Sim.Enable) Some(IO(Output(UInt(Config.Csr.DataWidth.W)))) else None
  val ioMstatus = if (Config.Sim.Enable) Some(IO(Output(UInt(Config.Csr.DataWidth.W)))) else None
  val ioMtvec   = if (Config.Sim.Enable) Some(IO(Output(UInt(Config.Csr.DataWidth.W)))) else None

  val dataOut    = WireDefault(0.U(Config.Csr.DataWidth.W))
  val dataIn     = WireDefault(0.U(Config.Csr.DataWidth.W))
  val preFlushPC = WireDefault(0.U(Config.Csr.DataWidth.W))
  val flushPC    = WireDefault(0.U(Config.Csr.DataWidth.W))

  val mod     = RegInit("b11".U(2.W))
  val MEPC    = RegInit((new BundleMEPC).initVal)
  val MCAUSE  = RegInit((new BundleMCAUSE).initVal)
  val MSTATUS = RegInit((new BundleMSTATUS).initVal)
  val MTVEC   = RegInit((new BundleMTVEC).initVal)
  val MIE     = RegInit(0.U(Config.Csr.DataWidth.W))
  val SIE     = RegInit(0.U(Config.Csr.DataWidth.W))
  val STVEC   = RegInit((new BundleMTVEC).initVal)
  val SSCRATCH = RegInit(0.U(Config.Csr.DataWidth.W))
  val SEPC    = RegInit((new BundleMEPC).initVal)
  val SCAUSE  = RegInit((new BundleMCAUSE).initVal)
  val STVAL   = RegInit(0.U(Config.Csr.DataWidth.W))
  val SATP    = RegInit(0.U(Config.Csr.DataWidth.W))
  val MEDELEG = RegInit(0.U(Config.Csr.DataWidth.W))
  val MIDELEG = RegInit(0.U(Config.Csr.DataWidth.W))

  val MIP = Cat(0.U(20.W), ioIrq.mei, 0.U(3.W), ioIrq.mti, 0.U(3.W), ioIrq.msi, 0.U(3.W))
  val timerIrq = MIE(7) && ioIrq.mti && MSTATUS.MIE(0)
  val softIrq  = MIE(3) && ioIrq.msi && MSTATUS.MIE(0)
  val extIrq   = MIE(11) && ioIrq.mei && MSTATUS.MIE(0)
  val sTimerIrq = SIE(5) && ioIrq.mti && MSTATUS.SIE(0)
  val sSoftIrq  = SIE(1) && ioIrq.msi && MSTATUS.SIE(0)
  val sExtIrq   = SIE(9) && ioIrq.mei && MSTATUS.SIE(0)
  val irqPend  = timerIrq || softIrq || extIrq || sTimerIrq || sSoftIrq || sExtIrq

  val ecallCause = MuxCase(
    0.U(Config.Csr.DataWidth.W),
    Seq(((mod === "b00".U) -> 0x8.U), ((mod === "b01".U) -> 0x9.U), ((mod === "b11".U) -> 0xb.U))
  )
  val ecallToS = mod =/= 3.U && MEDELEG(ecallCause(3, 0))
  val csrIllegal = ioCSRIDU.csr.en &&
    ((mod =/= 3.U && ioCSRIDU.csr.addr(9, 8) === "b11".U) ||
      (mod === 0.U && ioCSRIDU.csr.addr(9, 8) === "b01".U))
  val illegalToS = mod =/= 3.U && MEDELEG(2)
  val irqToS   = mod =/= 3.U && MIDELEG(ioIrqCauseIn(3, 0))

  dataOut := MuxCase(
    0.U(Config.Csr.DataWidth.W),
    Seq(
      (ioCSRIDU.csr.en && ioCSRIDU.csr.addr === CSR_MAP.MEPC)    -> MEPC.asUInt,
      (ioCSRIDU.csr.en && ioCSRIDU.csr.addr === CSR_MAP.MCAUSE)  -> MCAUSE.asUInt,
      (ioCSRIDU.csr.en && ioCSRIDU.csr.addr === CSR_MAP.MSTATUS) -> MSTATUS.asUInt,
      (ioCSRIDU.csr.en && ioCSRIDU.csr.addr === CSR_MAP.MIE)     -> MIE,
      (ioCSRIDU.csr.en && ioCSRIDU.csr.addr === CSR_MAP.MIP)     -> MIP,
      (ioCSRIDU.csr.en && ioCSRIDU.csr.addr === CSR_MAP.MTVEC)   -> MTVEC.asUInt,
      (ioCSRIDU.csr.en && ioCSRIDU.csr.addr === CSR_MAP.SSTATUS)  -> MSTATUS.asUInt,
      (ioCSRIDU.csr.en && ioCSRIDU.csr.addr === CSR_MAP.SIE)      -> SIE,
      (ioCSRIDU.csr.en && ioCSRIDU.csr.addr === CSR_MAP.SIP)      -> MIP,
      (ioCSRIDU.csr.en && ioCSRIDU.csr.addr === CSR_MAP.STVEC)    -> STVEC.asUInt,
      (ioCSRIDU.csr.en && ioCSRIDU.csr.addr === CSR_MAP.SSCRATCH) -> SSCRATCH,
      (ioCSRIDU.csr.en && ioCSRIDU.csr.addr === CSR_MAP.SEPC)     -> SEPC.asUInt,
      (ioCSRIDU.csr.en && ioCSRIDU.csr.addr === CSR_MAP.SCAUSE)   -> SCAUSE.asUInt,
      (ioCSRIDU.csr.en && ioCSRIDU.csr.addr === CSR_MAP.STVAL)    -> STVAL,
      (ioCSRIDU.csr.en && ioCSRIDU.csr.addr === CSR_MAP.SATP)     -> SATP,
      (ioCSRIDU.csr.en && ioCSRIDU.csr.addr === CSR_MAP.MEDELEG)  -> MEDELEG,
      (ioCSRIDU.csr.en && ioCSRIDU.csr.addr === CSR_MAP.MIDELEG)  -> MIDELEG
    )
  )

  dataIn := ioCSRWBU.csr.data

  when(ioCSRWBU.csr.en) {
    switch(ioCSRWBU.csr.addr) {
      is(CSR_MAP.MEPC) {
        MEPC := dataIn.asTypeOf(MEPC)
      }
      is(CSR_MAP.MCAUSE) {
        MCAUSE := dataIn.asTypeOf(MCAUSE)
      }
      is(CSR_MAP.MSTATUS) {
        MSTATUS := dataIn.asTypeOf(MSTATUS)
      }
      is(CSR_MAP.MIE) {
        MIE := dataIn
      }
      is(CSR_MAP.MTVEC) {
        MTVEC := dataIn.asTypeOf(MTVEC)
      }
      is(CSR_MAP.SSTATUS) {
        MSTATUS := dataIn.asTypeOf(MSTATUS)
      }
      is(CSR_MAP.SIE) {
        SIE := dataIn
      }
      is(CSR_MAP.STVEC) {
        STVEC := dataIn.asTypeOf(STVEC)
      }
      is(CSR_MAP.SSCRATCH) {
        SSCRATCH := dataIn
      }
      is(CSR_MAP.SEPC) {
        SEPC := dataIn.asTypeOf(SEPC)
      }
      is(CSR_MAP.SCAUSE) {
        SCAUSE := dataIn.asTypeOf(SCAUSE)
      }
      is(CSR_MAP.STVAL) {
        STVAL := dataIn
      }
      is(CSR_MAP.SATP) {
        SATP := dataIn
      }
      is(CSR_MAP.MEDELEG) {
        MEDELEG := dataIn
      }
      is(CSR_MAP.MIDELEG) {
        MIDELEG := dataIn
      }
    }
  }

  switch(ioCSRWBU.excType) {
    is(exc.ECALL) {
      when(ecallToS) {
        SEPC         := ioCSRWBU.pc.asTypeOf(SEPC)
        SCAUSE       := ecallCause.asTypeOf(SCAUSE)
        STVAL        := 0.U
        MSTATUS.SPIE := MSTATUS.SIE
        MSTATUS.SIE  := 0.U
        MSTATUS.SPP  := mod(0)
        mod          := "b01".U
      }.otherwise {
        MEPC         := ioCSRWBU.pc.asTypeOf(MEPC)
        MCAUSE       := ecallCause.asTypeOf(MCAUSE)
        MSTATUS.MPIE := MSTATUS.MIE
        MSTATUS.MIE  := 0.U
        MSTATUS.MPP  := mod
        mod          := "b11".U
      }
    }
    is(exc.ILLEGAL_INST) {
      when(illegalToS) {
        SEPC         := ioCSRWBU.pc.asTypeOf(SEPC)
        SCAUSE       := 2.U.asTypeOf(SCAUSE)
        STVAL        := 0.U
        MSTATUS.SPIE := MSTATUS.SIE
        MSTATUS.SIE  := 0.U
        MSTATUS.SPP  := mod(0)
        mod          := "b01".U
      }.otherwise {
        MEPC         := ioCSRWBU.pc.asTypeOf(MEPC)
        MCAUSE       := 2.U.asTypeOf(MCAUSE)
        MSTATUS.MPIE := MSTATUS.MIE
        MSTATUS.MIE  := 0.U
        MSTATUS.MPP  := mod
        mod          := "b11".U
      }
    }
    is(exc.MRET) {
      mod          := MSTATUS.MPP
      MSTATUS.MIE  := MSTATUS.MPIE
      MSTATUS.MPIE := 1.U
      MSTATUS.MPP  := "b00".U
    }
    is(exc.FENCE_I) {
      // TODO: FENCE_I
    }
    is(exc.SRET) {
      mod          := MSTATUS.SPP
      MSTATUS.SIE  := MSTATUS.SPIE
      MSTATUS.SPIE := 1.U
      MSTATUS.SPP  := 0.U
    }
    is(exc.FENCE) {
      // TODO: FENCE
    }
    is(exc.SFENCE_VMA) {
      // TODO: SFENCE_VMA
    }
    is(exc.WFI) {
      // TODO: WFI
    }
  }

  when(ioIrqTake) {
    when(irqToS) {
      SEPC         := ioIrqMepc.asTypeOf(SEPC)
      SCAUSE       := ioIrqCauseIn.asTypeOf(SCAUSE)
      STVAL        := 0.U
      MSTATUS.SPIE := MSTATUS.SIE
      MSTATUS.SIE  := 0.U
      MSTATUS.SPP  := mod(0)
      mod          := "b01".U
    }.otherwise {
      MEPC         := ioIrqMepc.asTypeOf(MEPC)
      MCAUSE       := ioIrqCauseIn.asTypeOf(MCAUSE)
      MSTATUS.MPIE := MSTATUS.MIE
      MSTATUS.MIE  := 0.U
      MSTATUS.MPP  := "b11".U
      mod          := "b11".U
    }
  }

  preFlushPC := MuxCase(
    0.U(Config.Addr.Width.W),
    Seq(
      (ioCSRIDU.excType === exc.MRET)  -> MEPC.asUInt,
      (ioCSRIDU.excType === exc.SRET)  -> SEPC.asUInt,
      (ioCSRIDU.excType === exc.ILLEGAL_INST) -> Mux(illegalToS, STVEC.asUInt, MTVEC.asUInt),
      (ioCSRIDU.excType === exc.ECALL) -> Mux(ecallToS, STVEC.asUInt, MTVEC.asUInt)
    )
  )

  flushPC := MuxCase(
    0.U(Config.Addr.Width.W),
    Seq(
      (ioCSRWBU.excType === exc.MRET)  -> MEPC.asUInt,
      (ioCSRWBU.excType === exc.SRET)  -> SEPC.asUInt,
      (ioCSRWBU.excType === exc.ILLEGAL_INST) -> Mux(illegalToS, STVEC.asUInt, MTVEC.asUInt),
      (ioCSRWBU.excType === exc.ECALL) -> Mux(ecallToS, STVEC.asUInt, MTVEC.asUInt)
    )
  )

  ioCSRIDU.preFlushPC := preFlushPC
  ioCSRWBU.flushPC    := flushPC
  ioIrqPending       := irqPend
  ioIrqCause         := MuxCase(
    "h8000_0003".U,
    Seq(
      timerIrq  -> "h8000_0007".U,
      sTimerIrq -> "h8000_0005".U,
      extIrq    -> "h8000_000b".U,
      sExtIrq   -> "h8000_0009".U,
      sSoftIrq  -> "h8000_0001".U
    )
  )
  ioIrqTrapPC        := Mux(irqToS, STVEC.asUInt, MTVEC.asUInt)

  ioCSRIDU.csr.data    := dataOut
  ioCSRIDU.csr.illegal := csrIllegal
  ioPriv.foreach(_ := mod)
  ioMepc.foreach(_ := MEPC.asUInt)
  ioMcause.foreach(_ := MCAUSE.asUInt)
  ioMstatus.foreach(_ := MSTATUS.asUInt)
  ioMtvec.foreach(_ := MTVEC.asUInt)
}
