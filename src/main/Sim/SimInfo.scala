package Sim

import chisel3._
import chisel3.util._

import Core.Config.Config
import Core.PCU.module._
import Core.IFU.module._
import Core.IDU.module._
import Core.EXU.module._

// SimInfo：非提交流的整机可观测面。
// 逐级 PipeStage 快照 + RegFile/CSRFile 镜像（DPI set_gpr_ptr / set_csr_ptr）。
class SimInfo extends Module {
  val io = IO(new Bundle {
    val pc_if_valid = Input(Bool())
    val pc_if       = Flipped(new PCUStageBundle)
    val if_id_valid = Input(Bool())
    val if_id       = Flipped(new IFUStageBundle)
    val id_ex_valid = Input(Bool())
    val id_ex       = Flipped(new IDUStageBundle)
    val ex_wb_valid = Input(Bool())
    val ex_wb       = Flipped(new EXUStageBundle)

    // 寄存器 / CSR 写回端口（用于镜像）
    val regEn   = Input(Bool())
    val regAddr = Input(UInt(Config.Reg.NumWidth.W))
    val regData = Input(UInt(Config.Reg.Width.W))
    val csrEn   = Input(Bool())
    val csrAddr = Input(UInt(Config.Csr.AddrWidth.W))
    val csrData = Input(UInt(Config.Csr.DataWidth.W))
  })

  // RegFile 镜像（DPI set_gpr_ptr）
  val regTrace = Module(new RegFileTrace)
  regTrace.io.clock := clock
  regTrace.io.reset := reset
  regTrace.io.en    := io.regEn
  regTrace.io.addr  := io.regAddr
  regTrace.io.data  := io.regData

  // CSRFile 镜像（DPI set_csr_ptr）
  val csrTrace = Module(new CsrFileTrace)
  csrTrace.io.clock := clock
  csrTrace.io.reset := reset
  csrTrace.io.en    := io.csrEn
  csrTrace.io.addr  := io.csrAddr
  csrTrace.io.data  := io.csrData

  dontTouch(io)
}