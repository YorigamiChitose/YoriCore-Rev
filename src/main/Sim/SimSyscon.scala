package Sim

import chisel3._

import Core.Device.SysconEvent

class SimSyscon extends BlackBox {
  override def desiredName = "SimSyscon"
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Reset())
    val exit  = Input(new SysconEvent)
  })
}
