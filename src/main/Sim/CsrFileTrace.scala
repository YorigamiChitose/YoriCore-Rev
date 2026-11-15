package Sim

import chisel3._
import chisel3.util._

import Core.Config.Config

// CSR 镜像黑盒（对应 resources/CsrFileTrace.sv，DPI set_csr_ptr）
class CsrFileTrace extends BlackBox {
  override def desiredName = "CsrFileTrace"
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Reset())
    val en    = Input(Bool())
    val addr  = Input(UInt(Config.Csr.AddrWidth.W))
    val data  = Input(UInt(Config.Csr.DataWidth.W))
  })
}