package Core.Device

import chisel3._
import chisel3.util._

import Core.Config.Config

class HaltEvent extends Bundle {
  val valid = Bool()
  val code  = UInt(31.W)
}

class Halt(val dataWidth: Int = Config.Bus.AxiDataWidth) extends Module {
  val io = IO(new Bundle {
    val bus  = Flipped(new DeviceBus(dataWidth))
    val halt = Output(new HaltEvent)
  })

  val pending = RegInit(false.B)
  val rdata   = RegInit(0.U(dataWidth.W))
  val rerr    = RegInit(false.B)

  val hit = io.bus.req.bits.addr(11, 0) === 0.U
  val wr  = io.bus.req.bits.write && io.bus.req.bits.wstrb(0) && io.bus.req.bits.wdata(0)

  io.bus.req.ready     := !pending
  io.bus.resp.valid    := pending
  io.bus.resp.bits.rdata := rdata
  io.bus.resp.bits.err   := rerr

  io.halt.valid := false.B
  io.halt.code  := 0.U

  when(io.bus.req.fire) {
    pending := true.B
    rdata   := 0.U
    rerr    := !hit
    when(hit && wr) {
      io.halt.valid := true.B
      io.halt.code  := io.bus.req.bits.wdata(62, 1)
    }
  }

  when(io.bus.resp.fire) {
    pending := false.B
  }
}