package Core.Device

import chisel3._
import chisel3.util._

import Core.Config.Config

class SysconEvent extends Bundle {
  val valid  = Bool()
  val reboot = Bool()
}

class Syscon(val dataWidth: Int = Config.Bus.AxiDataWidth) extends Module {
  val io = IO(new Bundle {
    val bus  = Flipped(new DeviceBus(dataWidth))
    val exit = Output(new SysconEvent)
  })

  val pending = RegInit(false.B)
  val rdata   = RegInit(0.U(dataWidth.W))
  val rerr    = RegInit(false.B)

  val hit  = io.bus.req.bits.addr(11, 0) === 0.U
  val word = io.bus.req.bits.wdata(15, 0)

  io.bus.req.ready      := !pending
  io.bus.resp.valid     := pending
  io.bus.resp.bits.rdata := rdata
  io.bus.resp.bits.err   := rerr

  io.exit.valid  := false.B
  io.exit.reboot := false.B

  when(io.bus.req.fire) {
    pending := true.B
    rdata   := 0.U
    rerr    := !hit
    when(hit && io.bus.req.bits.write && io.bus.req.bits.wstrb(0)) {
      when(word === 0x5555.U) {
        io.exit.valid := true.B
      }.elsewhen(word === 0x7777.U) {
        io.exit.valid  := true.B
        io.exit.reboot := true.B
      }
    }
  }

  when(io.bus.resp.fire) {
    pending := false.B
  }
}
