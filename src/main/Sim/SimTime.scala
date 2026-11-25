package Sim

import chisel3._

class SimTime extends BlackBox {
  override def desiredName = "SimTime"
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Reset())
    val rtcTime = Output(UInt(64.W))
  })
}
