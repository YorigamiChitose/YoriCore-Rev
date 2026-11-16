package Sim

import chisel3._
import chisel3.util._

import Core.Config.Config

// 提交观测黑盒（对应 resources/SimCommit.sv，DPI sim_commit）。
class SimCommit extends BlackBox {
  override def desiredName = "SimCommit"
  val io = IO(new Bundle {
    val clock    = Input(Clock())
    val reset    = Input(Reset())
    val valid    = Input(Bool())
    val pc       = Input(UInt(Config.Addr.Width.W))
    val npc      = Input(UInt(Config.Addr.Width.W))
    val inst     = Input(UInt(Config.Inst.Width.W))
    val priv     = Input(UInt(2.W))
    val exc      = Input(UInt(4.W))
    val rdEn     = Input(Bool())
    val rdAddr   = Input(UInt(Config.Reg.NumWidth.W))
    val rdData   = Input(UInt(Config.Reg.Width.W))
    val csrEn    = Input(Bool())
    val csrAddr  = Input(UInt(Config.Csr.AddrWidth.W))
    val csrData  = Input(UInt(Config.Csr.DataWidth.W))
    val memValid = Input(Bool())
    val memWrite = Input(Bool())
    val memAddr  = Input(UInt(Config.Addr.Width.W))
    val memWdata = Input(UInt(Config.Data.XLEN.W))
    val memSize  = Input(UInt(2.W))
  })
}
