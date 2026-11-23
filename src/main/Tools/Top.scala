package Tools

import chisel3._
import chisel3.util._

import Core._
import Core.Config.Config
import Core.Bus._

// SoC 边界：Core（内含 L1 I$/D$ + 仲裁）对外一个 MemPort，
// 由本层 AXI4Adapter 转换为标准 AXI4 主端口，后续在此挂接存储与外设。
class Top extends Module {
  val core       = Module(new Core)
  val axiAdapter = Module(new AXI4Adapter)

  // Core 的唯一访存端口 -> 边界协议转换
  core.ioMemPort <> axiAdapter.io.mem

  // 对外唯一标准 AXI4 主端口
  val ioAxi = IO(new AXI4Master(Config.Bus.AxiDataWidth))
  ioAxi <> axiAdapter.io.axi

  // 仿真可观测 tap 透传（总开关）
  val simTap = if (Config.Sim.Enable) Some(IO(new CoreSimTapBundle)) else None
  simTap.foreach { s => s := core.simTap.get }
}
