package Core

import chisel3._
import chisel3.util._

import Core.Config.Config
import Core.IDU.module._

// 仿真可观测 tap：仅提交信息 + 实际 CSR 状态。
// 仅当 Config.Sim.Enable 时由 Core/Top/SimTop 实例化；关闭时完全消除。
class CoreSimTapBundle extends Bundle {
  val commit = new Bundle {
    val valid   = Output(Bool())
    val pc      = Output(UInt(Config.Addr.Width.W))
    val npc     = Output(UInt(Config.Addr.Width.W))
    val inst    = Output(UInt(Config.Inst.Width.W))
    val priv    = Output(UInt(2.W))
    val excType = Output(exc())
    val rdEn    = Output(Bool())
    val rdAddr  = Output(UInt(Config.Reg.NumWidth.W))
    val rdData  = Output(UInt(Config.Reg.Width.W))
    val csrEn   = Output(Bool())
    val csrAddr = Output(UInt(Config.Csr.AddrWidth.W))
    val csrData = Output(UInt(Config.Csr.DataWidth.W))
    val memValid = Output(Bool())
    val memWrite = Output(Bool())
    val memAddr  = Output(UInt(Config.Addr.Width.W))
    val memWdata = Output(UInt(Config.Data.XLEN.W))
    val memSize  = Output(UInt(2.W)) // MemSize.B/H/W
  }

  // CsrFile 的实际状态（由 SimCsr 黑盒镜像给 C++）
  val csr = new Bundle {
    val mepc    = Output(UInt(Config.Csr.DataWidth.W))
    val mcause  = Output(UInt(Config.Csr.DataWidth.W))
    val mstatus = Output(UInt(Config.Csr.DataWidth.W))
    val mtvec   = Output(UInt(Config.Csr.DataWidth.W))
    val priv    = Output(UInt(2.W))
  }
}
