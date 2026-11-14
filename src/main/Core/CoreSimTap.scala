package Core

import chisel3._
import chisel3.util._

import Core.Config.Config
import Core.IDU.module._
import Core.PCU.module._
import Core.IFU.module._
import Core.EXU.module._

// 仿真可观测 tap：提交流 + 逐级 PipeStage 快照。
// 仅当 Config.Sim.Enable 时由 Core/Top 实例化，关闭则完全消除。
class CoreSimTapBundle extends Bundle {
  // 提交流（WBU 实际提交的指令信息）
  val commit = new Bundle {
    val valid   = Output(Bool())                       // 本拍提交
    val pc      = Output(UInt(Config.Addr.Width.W))    // 提交 PC
    val excType = Output(exc())                        // 异常类型
    val rdEn    = Output(Bool())                       // 写 rd
    val rdAddr  = Output(UInt(Config.Reg.NumWidth.W))  // rd 地址
    val rdData  = Output(UInt(Config.Reg.Width.W))     // rd 数据
    val csrEn   = Output(Bool())                       // 写 csr
    val csrAddr = Output(UInt(Config.Csr.AddrWidth.W)) // csr 地址
    val csrData = Output(UInt(Config.Csr.DataWidth.W)) // csr 数据
  }
  // 逐级 PipeStage 快照（流水级间传递的 StageBundle + valid）
  val pc_if_valid = Output(Bool())
  val pc_if       = new PCUStageBundle
  val if_id_valid = Output(Bool())
  val if_id       = new IFUStageBundle
  val id_ex_valid = Output(Bool())
  val id_ex       = new IDUStageBundle
  val ex_wb_valid = Output(Bool())
  val ex_wb       = new EXUStageBundle
}