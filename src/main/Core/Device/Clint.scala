package Core.Device

import chisel3._
import chisel3.util._

import Core.Config.Config

class ClintIrq extends Bundle {
  val msi = Bool()
  val mti = Bool()
}

class Clint(val dataWidth: Int = Config.Bus.AxiDataWidth) extends Module {
  val io = IO(new Bundle {
    val bus = Flipped(new DeviceBus(dataWidth))
    val irq = Output(new ClintIrq)
  })

  val pending = RegInit(false.B)
  val rdata   = RegInit(0.U(dataWidth.W))
  val rerr    = RegInit(false.B)

  val mtime    = RegInit(0.U(64.W))
  val mtimecmp = RegInit("hffff_ffff_ffff_ffff".U(64.W))
  val msip     = RegInit(0.U(32.W))

  mtime := mtime + 1.U

  val off    = io.bus.req.bits.addr(15, 0)
  val msipEn = off === 0x0000.U
  val cmpLo  = off === 0x4000.U
  val cmpHi  = off === 0x4004.U
  val timeLo = off === 0xbff8.U
  val timeHi = off === 0xbffc.U
  val hit    = msipEn || cmpLo || cmpHi || timeLo || timeHi

  val cmp = Cat(mtimecmp(63, 32), mtimecmp(31, 0))
  io.irq.msi := msip(0)
  io.irq.mti := mtime >= cmp

  io.bus.req.ready      := !pending
  io.bus.resp.valid     := pending
  io.bus.resp.bits.rdata := rdata
  io.bus.resp.bits.err   := rerr

  when(io.bus.req.fire) {
    pending := true.B
    rerr    := !hit
    rdata   := Mux(
      msipEn,
      Cat(0.U(32.W), msip),
      Mux(
        cmpLo,
        Cat(0.U(32.W), mtimecmp(31, 0)),
        Mux(
          cmpHi,
          Cat(0.U(32.W), mtimecmp(63, 32)),
          Mux(timeLo, Cat(0.U(32.W), mtime(31, 0)), Mux(timeHi, Cat(0.U(32.W), mtime(63, 32)), 0.U))
        )
      )
    )
    when(io.bus.req.bits.write) {
      when(msipEn && io.bus.req.bits.wstrb(0)) { msip := io.bus.req.bits.wdata(31, 0) }
      when(cmpLo && io.bus.req.bits.wstrb.orR) { mtimecmp := Cat(mtimecmp(63, 32), io.bus.req.bits.wdata(31, 0)) }
      when(cmpHi && io.bus.req.bits.wstrb.orR) { mtimecmp := Cat(io.bus.req.bits.wdata(31, 0), mtimecmp(31, 0)) }
    }
  }

  when(io.bus.resp.fire) {
    pending := false.B
  }
}
