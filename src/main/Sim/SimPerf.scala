package Sim

import chisel3._
import Core.Config.Config

class SimPerf extends BlackBox {
  override def desiredName = "SimPerf"
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Reset())
    val valid = Input(Bool())
    val inst  = Input(UInt(Config.Inst.Width.W))
    val iHit  = Input(Bool())
    val iMiss = Input(Bool())
    val dHit  = Input(Bool())
    val dMiss = Input(Bool())
  })
}
