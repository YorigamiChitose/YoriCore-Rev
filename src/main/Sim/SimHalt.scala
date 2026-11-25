package Sim

import chisel3._

import Core.Config.Config
import Core.Device.HaltEvent

class SimHalt extends BlackBox(Map("HALT_CODE_WIDTH" -> Config.Device.HaltCodeWidth)) {
  override def desiredName = "SimHalt"
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Reset())
    val halt  = Input(new HaltEvent)
  })
}
