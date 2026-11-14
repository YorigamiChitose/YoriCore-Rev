package Tools

import chisel3._
import chisel3.util._

import Core._
import Sim._

// 仿真顶层：核心 Top（含 I$/D$ + 单一 AXI 主端口）+ 外部 AXI4 从设备存储模型
class SimTop extends Module {
  val core = Module(new Top)
  val ram  = Module(new SimAXIRam)

  ram.io.clock := clock
  ram.io.reset := reset

  core.ioAxi <> ram.io.axi
}