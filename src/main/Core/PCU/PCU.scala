package Core.PCU

import chisel3._
import chisel3.util._

import Core.Config.Config
import Core.PCU.module._
import Core.Pipe.module._

// PCU 决定PC值
class PCU extends Module {
  val ioCtrl          = IO(new PCUCtrlBundle())                // 控制信号
  val ioPCU           = IO(new PCUStageBundle())               // 下级流水信号
  val ioPCUForwarding = IO(Flipped(new PCUForwardingBundle())) // 来自 Forwarding 的下一顺序 PC

  val pc     = WireDefault(0.U(Config.Addr.Width.W))                    // pc线
  val nextPc = WireDefault(0.U(Config.Addr.Width.W))                    // 下一pc值
  val pcReg  = RegNext(nextPc, Config.Addr.Base.U(Config.Addr.Width.W)) // pc寄存器

  // 冲刷时: 使用flushPC 默认时: 未就绪或停止则保持, 否则步进（由 Forwarding 给出 +2/+4）
  nextPc := Mux(
    ioCtrl.pipe.flush,
    ioCtrl.flushPC,
    Mux(ioCtrl.pipe.stall || !ioPCUForwarding.ready, pc, ioPCUForwarding.nextSeqPc)
  )
  pc     := pcReg

  // Pipe控制
  ioCtrl.pipe.valid := true.B // PCU准备信号默认一直为有效

  // PCU IO
  // 注意：输出 nextPc（组合逻辑），由 PC_IF 级间寄存器单次寄存，
  // 避免与 pcReg 形成双重寄存导致 PC 步进滞后一拍、指令被重复译码。
  ioPCU.pc := nextPc // 向后级传递下一PC
}
