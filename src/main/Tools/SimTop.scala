package Tools

import chisel3._
import chisel3.util._

import Core._
import Core.Config.Config
import Sim._

// 仿真顶层：Top（核心 + I$/D$ + 单一 AXI 主端口）+ AXI4 从设备存储模型
// + 仿真可观测层（SMU / SimInfo，总开关 Config.Sim.Enable）
class SimTop extends Module {
  val core = Module(new Top)
  val ram  = Module(new SimAXIRam)

  ram.io.clock := clock
  ram.io.reset := reset

  core.ioAxi <> ram.io.axi

  // 观测层（仅当 Config.Sim.Enable）
  val ioSimEbreak = if (Config.Sim.Enable) Some(IO(Output(Bool()))) else None
  if (Config.Sim.Enable) {
    val smu     = Module(new SMU)
    val simInfo = Module(new SimInfo)
    val tap     = core.simTap.get

    smu.io.commitValid := tap.commit.valid
    smu.io.commitPc    := tap.commit.pc
    smu.io.commitExc   := tap.commit.excType
    smu.io.rdEn        := tap.commit.rdEn
    smu.io.rdAddr      := tap.commit.rdAddr
    smu.io.rdData      := tap.commit.rdData
    smu.io.csrEn       := tap.commit.csrEn
    smu.io.csrAddr     := tap.commit.csrAddr
    smu.io.csrData     := tap.commit.csrData
    ioSimEbreak.get := smu.io.ebreak

    simInfo.io.pc_if_valid := tap.pc_if_valid
    simInfo.io.pc_if       := tap.pc_if
    simInfo.io.if_id_valid := tap.if_id_valid
    simInfo.io.if_id       := tap.if_id
    simInfo.io.id_ex_valid := tap.id_ex_valid
    simInfo.io.id_ex       := tap.id_ex
    simInfo.io.ex_wb_valid := tap.ex_wb_valid
    simInfo.io.ex_wb       := tap.ex_wb

    simInfo.io.regEn   := tap.commit.valid && tap.commit.rdEn
    simInfo.io.regAddr := tap.commit.rdAddr
    simInfo.io.regData := tap.commit.rdData
    simInfo.io.csrEn   := tap.commit.valid && tap.commit.csrEn
    simInfo.io.csrAddr := tap.commit.csrAddr
    simInfo.io.csrData := tap.commit.csrData
  }
}