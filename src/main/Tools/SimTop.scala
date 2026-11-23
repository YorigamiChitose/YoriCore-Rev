package Tools

import chisel3._
import chisel3.util._

import Core._
import Core.Config.Config
import Sim._

// 仿真顶层：Top（Core 含 Cache/Arbiter + AXI Adapter）+ AXI 存储模型
// + 仅仿真观测黑盒（SimCommit / SimCsr）。
// C++ monitor 通过 DPI 获取提交与 CSR 状态；核心不导出仿真端口。
class SimTop extends Module {
  val core = Module(new Top)
  val ram  = Module(new SimAXIRam(Config.Sim.AxiLatency))

  ram.io.clock := clock
  ram.io.reset := reset

  core.ioAxi <> ram.io.axi

  if (Config.Sim.Enable) {
    val tap = core.simTap.get

    val commit = Module(new SimCommit)
    commit.io.clock    := clock
    commit.io.reset    := reset
    commit.io.valid    := tap.commit.valid
    commit.io.pc       := tap.commit.pc
    commit.io.npc      := tap.commit.npc
    commit.io.inst     := tap.commit.inst
    commit.io.priv     := tap.commit.priv
    commit.io.exc      := tap.commit.excType.asUInt
    commit.io.rdEn     := tap.commit.rdEn && (tap.commit.rdAddr =/= 0.U)
    commit.io.rdAddr   := tap.commit.rdAddr
    commit.io.rdData   := tap.commit.rdData
    commit.io.csrEn    := tap.commit.csrEn
    commit.io.csrAddr  := tap.commit.csrAddr
    commit.io.csrData  := tap.commit.csrData
    commit.io.memValid := tap.commit.memValid
    commit.io.memWrite := tap.commit.memWrite
    commit.io.memAddr  := tap.commit.memAddr
    commit.io.memWdata := tap.commit.memWdata
    commit.io.memSize  := tap.commit.memSize

    val perf = Module(new SimPerf)
    perf.io.clock := clock
    perf.io.reset := reset
    perf.io.valid := tap.commit.valid
    perf.io.inst  := tap.commit.inst
    perf.io.iHit  := tap.perf.iHit
    perf.io.iMiss := tap.perf.iMiss
    perf.io.dHit  := tap.perf.dHit
    perf.io.dMiss := tap.perf.dMiss

    val csr = Module(new SimCsr)
    csr.io.clock   := clock
    csr.io.reset   := reset
    csr.io.mepc    := tap.csr.mepc
    csr.io.mcause  := tap.csr.mcause
    csr.io.mstatus := tap.csr.mstatus
    csr.io.mtvec   := tap.csr.mtvec
    csr.io.priv    := tap.csr.priv
  }
}
