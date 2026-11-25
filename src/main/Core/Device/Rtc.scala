package Core.Device

import chisel3._
import chisel3.util._

import Core.Config.Config

class Rtc(val dataWidth: Int = Config.Bus.AxiDataWidth) extends Module {
  val io = IO(new Bundle {
    val bus  = Flipped(new DeviceBus(dataWidth))
    val time = Input(UInt(64.W))
  })

  val pending = RegInit(false.B)
  val rdata   = RegInit(0.U(dataWidth.W))
  val rerr    = RegInit(false.B)

  val off = io.bus.req.bits.addr(11, 0)
  val hit = off < 0x100.U

  io.bus.req.ready      := !pending
  io.bus.resp.valid     := pending
  io.bus.resp.bits.rdata := rdata
  io.bus.resp.bits.err   := rerr

  when(io.bus.req.fire) {
    pending := true.B
    rerr    := !hit
    rdata   := Mux(off === 0.U, Cat(0.U(32.W), io.time(31, 0)), Cat(0.U(32.W), io.time(63, 32)))
  }

  when(io.bus.resp.fire) {
    pending := false.B
  }
}
