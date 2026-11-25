package Core

import chisel3._
import chisel3.util._

import Core.Pipe._
import Core.Pipe.module._
import Core.PCU._
import Core.PCU.module._
import Core.IFU._
import Core.IFU.module._
import Core.IDU._
import Core.IDU.module._
import Core.EXU._
import Core.EXU.module._
import Core.WBU._
import Core.WBU.module._
import Core.Reg._
import Core.Reg.module._
import Core.Bus._
import Core.Cache._
import Core.Device.CoreIrq
import Core.Config.Config

class Core extends Module {
  val PCU   = Module(new PCU)
  val PC_IF = Module(new PipeStage(new PCUStageBundle))
  val IFU   = Module(new IFU)
  val IF_ID = Module(new PipeStage(new IFUStageBundle))
  val IDU   = Module(new IDU)
  val ID_EX = Module(new PipeStage(new IDUStageBundle))
  val EXU   = Module(new EXU)
  val EX_WB = Module(new PipeStage(new EXUStageBundle))
  val WBU   = Module(new WBU)

  val regFile = Module(new RegFile) // 通用寄存器
  val csrFile = Module(new CsrFile) // CSR寄存器

  val pipeLineCtrl = Module(new PipeLineCtrl) // 流水控制
  val forwording   = Module(new Forwarding)   // 前递寄存器

  val ioIrq = IO(Input(new CoreIrq))

  pipeLineCtrl.ioPCUCtrl <> PCU.ioCtrl // PCU - 流水控制器
  pipeLineCtrl.ioIFUCtrl <> IFU.ioCtrl // IFU - 流水控制器
  pipeLineCtrl.ioIDUCtrl <> IDU.ioCtrl // IDU - 流水控制器
  pipeLineCtrl.ioEXUCtrl <> EXU.ioCtrl // EXU - 流水控制器
  pipeLineCtrl.ioWBUCtrl <> WBU.ioCtrl // WBU - 流水控制器

  pipeLineCtrl.ioPC_IFPipeCtrl <> PC_IF.ioPipeCtrl // PC_IF级间寄存器 - 流水控制器
  pipeLineCtrl.ioIF_IDPipeCtrl <> IF_ID.ioPipeCtrl // IF_ID级间寄存器 - 流水控制器
  pipeLineCtrl.ioID_EXPipeCtrl <> ID_EX.ioPipeCtrl // ID_EX级间寄存器 - 流水控制器
  pipeLineCtrl.ioEX_WBPipeCtrl <> EX_WB.ioPipeCtrl // EX_WB级间寄存器 - 流水控制器

  PCU.ioPCU <> PC_IF.ioPrevStage // PCU - PC_IF 数据通路
  IFU.ioIFU <> IF_ID.ioPrevStage // IFU - IF_ID 数据通路
  IDU.ioIDU <> ID_EX.ioPrevStage // IDU - ID_EX 数据通路
  EXU.ioEXU <> EX_WB.ioPrevStage // EXU - EX_WB 数据通路

  PC_IF.ioNextStage <> IFU.ioPCU // PC_IF - IFU 数据通路
  IF_ID.ioNextStage <> IDU.ioIFU // IF_ID - IDU 数据通路
  ID_EX.ioNextStage <> EXU.ioIDU // ID_EX - EXU 数据通路
  EX_WB.ioNextStage <> WBU.ioEXU // EX_WB - WBU 数据通路

  IFU.ioValid <> PC_IF.ioValid // PC_IF 数据有效
  IDU.ioValid <> IF_ID.ioValid // IF_ID 数据有效
  EXU.ioValid <> ID_EX.ioValid // ID_EX 数据有效
  WBU.ioValid <> EX_WB.ioValid // EX_WB 数据有效

  IDU.ioRegIDU <> regFile.ioRegIDU // IDU - 通用寄存器 数据通路
  IDU.ioCSRIDU <> csrFile.ioCSRIDU // IDU - csr寄存器 数据通路

  WBU.ioRegWBU <> regFile.ioRegWBU // WBU - 通用寄存器 数据通路
  WBU.ioCSRWBU <> csrFile.ioCSRWBU // WBU - csr寄存器 数据通路

  csrFile.ioIrq := ioIrq
  pipeLineCtrl.ioPC_IFValid := PC_IF.ioValid
  pipeLineCtrl.ioIF_IDValid := IF_ID.ioValid
  pipeLineCtrl.ioID_EXValid := ID_EX.ioValid
  pipeLineCtrl.ioEX_WBValid := EX_WB.ioValid
  pipeLineCtrl.ioIrqPending := csrFile.ioIrqPending
  pipeLineCtrl.ioIrqCause   := csrFile.ioIrqCause
  pipeLineCtrl.ioIrqTrapPC  := csrFile.ioIrqTrapPC
  pipeLineCtrl.ioPcuPc      := PCU.ioPCU.pc
  pipeLineCtrl.ioWBUValid   := WBU.ioValid
  csrFile.ioIrqTake         := pipeLineCtrl.ioIrqTake
  csrFile.ioIrqMepc         := pipeLineCtrl.ioIrqMepc
  csrFile.ioIrqCauseIn      := pipeLineCtrl.ioIrqCauseOut


  forwording.ioPCU <> PCU.ioPCUForwarding // PCU - 前递 数据通路
  forwording.ioIFU <> IFU.ioIFUForwarding // IFU - 前递 数据通路
  forwording.ioIDU <> IDU.ioIDUForwarding // IDU - 前递 数据通路
  forwording.ioEXU <> EXU.ioEXUForwarding // EXU - 前递 数据通路
  forwording.ioWBU <> WBU.ioWBUForwarding // WBU - 前递 数据通路

  // L1 I$/D$ + 单一仲裁：Cache 属于 Core，对外仅一个 MemPort
  val iCache  = Module(new L1Cache)
  val dCache  = Module(new L1Cache)
  val arbiter = Module(new MemArbiter)

  IFU.ioMemPort <> iCache.io.cpu
  EXU.ioMemPort <> dCache.io.cpu

  val ioMemPort = IO(new MemPort)
  iCache.io.mem <> arbiter.io.in0
  dCache.io.mem <> arbiter.io.in1
  arbiter.io.out <> ioMemPort

  // fence.i 排序：D$ 写回排空完成前，I$ 暂停对外服务，避免读回旧指令
  val fenceOrder = RegInit(false.B)
  when(WBU.ioFenceI) { fenceOrder := true.B }
  when(dCache.io.fenceDone) { fenceOrder := false.B }
  iCache.io.fence := WBU.ioFenceI
  dCache.io.fence := WBU.ioFenceI
  iCache.io.hold  := fenceOrder
  dCache.io.hold  := false.B

  // 仿真可观测 tap（总开关 Config.Sim.Enable）
  val obsTap = if (Config.Sim.Enable) Some(IO(new CoreSimTapBundle)) else None
  obsTap.foreach { s =>
    s.obsCommit.valid    := WBU.ioValid
    s.obsCommit.pc       := WBU.ioEXU.pc
    s.obsCommit.npc      := WBU.ioEXU.obsNpc
    s.obsCommit.inst     := WBU.ioEXU.obsInst
    s.obsCommit.priv     := csrFile.ioPriv.get
    s.obsCommit.excType  := WBU.ioEXU.excType
    s.obsCommit.rdEn     := WBU.ioEXU.rdEn
    s.obsCommit.rdAddr   := WBU.ioEXU.rdAddr
    s.obsCommit.rdData   := WBU.ioEXU.EXUResult
    s.obsCommit.csrEn    := WBU.ioEXU.csrWriteEn
    s.obsCommit.csrAddr  := WBU.ioEXU.csrAddr
    s.obsCommit.csrData  := WBU.ioEXU.csrResult
    s.obsCommit.memValid := WBU.ioEXU.obsMemValid.asBool
    s.obsCommit.memWrite := WBU.ioEXU.obsMemWrite.asBool
    s.obsCommit.memAddr  := WBU.ioEXU.obsMemAddr
    s.obsCommit.memWdata := WBU.ioEXU.obsMemWdata
    s.obsCommit.memSize  := WBU.ioEXU.obsMemSize

    s.obsPerf.iHit  := iCache.obsHit.get
    s.obsPerf.iMiss := iCache.obsMiss.get
    s.obsPerf.dHit  := dCache.obsHit.get
    s.obsPerf.dMiss := dCache.obsMiss.get

    s.obsCsr.mepc    := csrFile.ioMepc.get
    s.obsCsr.mcause  := csrFile.ioMcause.get
    s.obsCsr.mstatus := csrFile.ioMstatus.get
    s.obsCsr.mtvec   := csrFile.ioMtvec.get
    s.obsCsr.priv    := csrFile.ioPriv.get
    s.obsCsr.sepc    := csrFile.ioSepc.get
    s.obsCsr.scause  := csrFile.ioScause.get
    s.obsCsr.stvec   := csrFile.ioStvec.get
    s.obsCsr.sie     := csrFile.ioSie.get
    s.obsCsr.sip     := csrFile.ioSip.get
    s.obsCsr.sstatus := csrFile.ioSstatus.get
    s.obsCsr.satp    := csrFile.ioSatp.get
  }
}
