package Sim

import chisel3._
import chisel3.util._

import Core.Config.Config

// CSR 状态观测黑盒（对应 resources/SimCsr.sv，DPI sim_csr_update）。
class SimCsr extends BlackBox {
  override def desiredName = "SimCsr"
  val io                   = IO(new Bundle {
    val clock   = Input(Clock())
    val reset   = Input(Reset())
    val mepc    = Input(UInt(Config.Csr.DataWidth.W))
    val mcause  = Input(UInt(Config.Csr.DataWidth.W))
    val mstatus = Input(UInt(Config.Csr.DataWidth.W))
    val mtvec   = Input(UInt(Config.Csr.DataWidth.W))
    val priv    = Input(UInt(2.W))
    val sepc    = Input(UInt(Config.Csr.DataWidth.W))
    val scause  = Input(UInt(Config.Csr.DataWidth.W))
    val stvec   = Input(UInt(Config.Csr.DataWidth.W))
    val sie     = Input(UInt(Config.Csr.DataWidth.W))
    val sip     = Input(UInt(Config.Csr.DataWidth.W))
    val sstatus = Input(UInt(Config.Csr.DataWidth.W))
    val satp    = Input(UInt(Config.Csr.DataWidth.W))
  })
}
