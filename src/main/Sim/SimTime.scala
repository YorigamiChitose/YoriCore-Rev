package Sim

import chisel3._

import Core.Config.Config

class SimTime extends BlackBox(Map("TIME_WIDTH" -> Config.Device.TimeWidth)) {
  override def desiredName = "SimTime"
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Reset())
    val rtcTime = Output(UInt(Config.Device.TimeWidth.W))
  })
}
