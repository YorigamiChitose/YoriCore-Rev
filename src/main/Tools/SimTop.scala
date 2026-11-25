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

  val simTime = Module(new SimTime)
  simTime.io.clock := clock
  simTime.io.reset := reset
  core.ioRtcTime := simTime.io.rtcTime
  core.ioPlicSources := 0.U

  val simSyscon = Module(new SimSyscon)
  simSyscon.io.clock := clock
  simSyscon.io.reset := reset
  simSyscon.io.exit  := core.ioSyscon

  val simUart = Module(new SimUart)
  simUart.io.clock := clock
  simUart.io.reset := reset
  simUart.io.tx    := core.ioUart

  val simHalt = Module(new SimHalt)
  simHalt.io.clock := clock
  simHalt.io.reset := reset
  simHalt.io.halt  := core.ioHalt

  if (Config.Sim.Enable) {
    val obsTap = core.obsTap.get

    val commit = Module(new SimCommit)
    commit.io.clock    := clock
    commit.io.reset    := reset
    commit.io.valid    := obsTap.obsCommit.valid
    commit.io.pc       := obsTap.obsCommit.pc
    commit.io.npc      := obsTap.obsCommit.npc
    commit.io.inst     := obsTap.obsCommit.inst
    commit.io.priv     := obsTap.obsCommit.priv
    commit.io.exc      := obsTap.obsCommit.excType.asUInt
    commit.io.rdEn     := obsTap.obsCommit.rdEn && (obsTap.obsCommit.rdAddr =/= 0.U)
    commit.io.rdAddr   := obsTap.obsCommit.rdAddr
    commit.io.rdData   := obsTap.obsCommit.rdData
    commit.io.csrEn    := obsTap.obsCommit.csrEn
    commit.io.csrAddr  := obsTap.obsCommit.csrAddr
    commit.io.csrData  := obsTap.obsCommit.csrData
    commit.io.memValid := obsTap.obsCommit.memValid
    commit.io.memWrite := obsTap.obsCommit.memWrite
    commit.io.memAddr  := obsTap.obsCommit.memAddr
    commit.io.memWdata := obsTap.obsCommit.memWdata
    commit.io.memSize  := obsTap.obsCommit.memSize

    val perf = Module(new SimPerf)
    perf.io.clock := clock
    perf.io.reset := reset
    perf.io.valid := obsTap.obsCommit.valid
    perf.io.inst  := obsTap.obsCommit.inst
    perf.io.iHit  := obsTap.obsPerf.iHit
    perf.io.iMiss := obsTap.obsPerf.iMiss
    perf.io.dHit  := obsTap.obsPerf.dHit
    perf.io.dMiss := obsTap.obsPerf.dMiss

    val csr = Module(new SimCsr)
    csr.io.clock   := clock
    csr.io.reset   := reset
    csr.io.mepc    := obsTap.obsCsr.mepc
    csr.io.mcause  := obsTap.obsCsr.mcause
    csr.io.mstatus := obsTap.obsCsr.mstatus
    csr.io.mtvec   := obsTap.obsCsr.mtvec
    csr.io.priv    := obsTap.obsCsr.priv
    csr.io.sepc    := obsTap.obsCsr.sepc
    csr.io.scause  := obsTap.obsCsr.scause
    csr.io.stvec   := obsTap.obsCsr.stvec
    csr.io.sie     := obsTap.obsCsr.sie
    csr.io.sip     := obsTap.obsCsr.sip
    csr.io.sstatus := obsTap.obsCsr.sstatus
    csr.io.satp    := obsTap.obsCsr.satp
  }
}
