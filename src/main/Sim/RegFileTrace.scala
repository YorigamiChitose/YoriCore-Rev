package Sim

import chisel3._
import chisel3.util._

import Core.Config.Config

// 通用寄存器镜像黑盒（对应 resources/RegFileTrace.sv，DPI set_gpr_ptr）
class RegFileTrace extends BlackBox {
  override def desiredName = "RegFileTrace"
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Reset())
    val en    = Input(Bool())
    val addr  = Input(UInt(Config.Reg.NumWidth.W))
    val data  = Input(UInt(Config.Reg.Width.W))
  })
}