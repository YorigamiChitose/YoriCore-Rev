package Sim

import chisel3._
import chisel3.util._

import Core.PCU.module._
import Core.IFU.module._
import Core.IDU.module._
import Core.EXU.module._

// SimInfo：非提交流的整机可观测面（逐级 PipeStage 快照）。
// M1 仅覆盖逐级 StageBundle + valid；M2 起并入 RegFile/CSRFile/PCU/Cache。
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
  })

  // 纯观测模块，无输出，防止被优化消除
  dontTouch(io)
}