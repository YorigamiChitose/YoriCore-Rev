package Core.Device

import chisel3._
import chisel3.util._

import Core.Config.Config

class UartTx extends Bundle {
  val valid = Bool()
  val data  = UInt(8.W)
}

class Uart(val dataWidth: Int = Config.Bus.AxiDataWidth) extends Module {
  val io = IO(new Bundle {
    val bus = Flipped(new DeviceBus(dataWidth))
    val tx  = Output(new UartTx)
    val irq = Output(Bool())
  })

  val pending = RegInit(false.B)
  val rdata   = RegInit(0.U(dataWidth.W))
  val rerr    = RegInit(false.B)
  val ier     = RegInit(0.U(8.W))
  val txPend  = RegInit(false.B)

  val off = io.bus.req.bits.addr(11, 0)
  val hit = off < 0x100.U
  val lsr = off === 5.U
  val iir = off === 2.U
  val ierEn = off === 1.U
  val thr = off === 0.U

  io.bus.req.ready      := !pending
  io.bus.resp.valid     := pending
  io.bus.resp.bits.rdata := rdata
  io.bus.resp.bits.err   := rerr

  io.tx.valid := false.B
  io.tx.data  := 0.U
  io.irq := ier(1) && txPend

  when(io.bus.req.fire) {
    pending := true.B
    rerr    := !hit
    rdata   := Mux(lsr, 0x60.U, Mux(iir, 0x01.U, 0.U))
    when(hit && io.bus.req.bits.write && ierEn) { ier := io.bus.req.bits.wdata(7, 0) }
    when(iir && !io.bus.req.bits.write) { txPend := false.B }
    when(hit && io.bus.req.bits.write && thr && io.bus.req.bits.wstrb(0)) {
      io.tx.valid := true.B
      io.tx.data  := io.bus.req.bits.wdata(7, 0)
      txPend := true.B
    }
  }

  when(io.bus.resp.fire) {
    pending := false.B
  }
}
