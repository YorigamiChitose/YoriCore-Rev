package Sim

import chisel3._

import Core.Device.UartTx

class SimUart extends BlackBox {
  override def desiredName = "SimUart"
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Reset())
    val tx    = Input(new UartTx)
  })
}
