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

  // 冲刷时: 使用flushPC 默认时: 确定是否被停止, 不是则步进（由 Forwarding 给出 +2/+4）
  nextPc := Mux(ioCtrl.pipe.flush, ioCtrl.flushPC, Mux(ioCtrl.pipe.stall, pc, ioPCUForwarding.nextSeqPc))
  pc     := pcReg

  // Pipe控制
  ioCtrl.pipe.valid := true.B // PCU准备信号默认一直为有效

  // PCU IO
  ioPCU.pc := pc // 向后级传递PC
}
