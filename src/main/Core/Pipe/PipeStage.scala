package Core.Pipe

import chisel3._
import chisel3.util._
import Core.Pipe.module._

// 级间寄存器
class PipeStage[T <: StageBundle](stageBundle: T) extends Module {
  val ioPipeCtrl  = IO(new PipeRegCtrlBundle()) // 控制信号
  val ioPrevStage = IO(Flipped(stageBundle))    // 前级传来的数据
  val ioNextStage = IO(stageBundle)             // 发送到后级的数据
  val ioValid     = IO(Output(Bool()))          // 数据有效信号

  // 流水级间寄存器
  val stageReg = RegInit(stageBundle, stageBundle.initVal())
  val validReg = RegInit(false.B) // 数据有效信号寄存器

  when(ioPipeCtrl.flush || (ioPipeCtrl.stallPrev && !ioPipeCtrl.stallNext)) {
    // 被命令冲刷 || 前级停止后级继续（后级正在排空）
    stageReg := stageBundle.initVal() // 发送默认数据
    validReg := false.B               // 设置valid为false
  }.elsewhen(ioPipeCtrl.stallPrev) {
    // 前级与后级同时停止：保持当前值（不能被前级气泡覆盖）
  }.elsewhen(!ioPipeCtrl.valid) {
    // 前级未完成：插入气泡
    stageReg := stageBundle.initVal()
    validReg := false.B
  }.otherwise {
    // 前级不停止：向后传输
    stageReg := ioPrevStage
    validReg := true.B
  }
  ioNextStage := stageReg // 后级数据
  ioValid     := validReg // 数据有效
}
