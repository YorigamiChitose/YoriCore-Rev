package Tools

import chisel3._
import chisel3.util._

import Core._
import Core.Config.Config
import Core.Bus._
import Core.Cache._

// 纯核心 + L1 I$/D$ + 单一仲裁 + 单一 AXI 适配器，对外唯一 AXI4 主端口。
class Top extends Module {
  val core       = Module(new Core)
  val iCache     = Module(new L1Cache)
  val dCache     = Module(new L1Cache)
  val arbiter    = Module(new MemArbiter)
  val axiAdapter = Module(new AXI4Adapter)

  // CPU 侧接 I$/D$（统一 MemPort）
  core.ioIMemPort <> iCache.io.cpu
  core.ioDMemPort <> dCache.io.cpu

  // fence.i 回写/失效
  iCache.io.fence := core.ioFenceI
  dCache.io.fence := core.ioFenceI

  // I$/D$ 统一存储侧 -> 单一仲裁 -> 单一 AXI 适配器
  iCache.io.mem <> arbiter.io.in0
  dCache.io.mem <> arbiter.io.in1
  arbiter.io.out <> axiAdapter.io.mem

  // 对外唯一 AXI4 主端口
  val ioAxi = IO(new AXI4Master(Config.Bus.AxiDataWidth))
  ioAxi <> axiAdapter.io.axi
}