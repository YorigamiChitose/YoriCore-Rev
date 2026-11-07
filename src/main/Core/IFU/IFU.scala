package Core.IFU

import chisel3._
import chisel3.util._

import Core.Config.Config
import Core.IFU.module._
import Core.PCU.module._
import Core.Pipe.module._

// IFU：支持 RV32C 的变长取指。
// 维护一个对齐字缓冲 + 跨字第二字缓冲，按 16/32 位组装指令并给出 pcInc。
class IFU extends Module {
  val ioCtrl          = IO(new IFUCtrlBundle())                // 控制信号
  val ioIFU           = IO(new IFUStageBundle())               // 下级流水信号
  val ioPCU           = IO(Flipped(new PCUStageBundle()))      // 上级流水信号
  val ioIMem          = IO(Flipped(new IMemBundle()))          // IFU-存储器
  val ioValid         = IO(Input(Bool()))                      // 上级数据有效信号
  val ioIFUForwarding = IO(Flipped(new IFUForwardingBundle())) // 送往 Forwarding 的顺序 PC 信息

  // 当前 PC 所在对齐字地址（清掉字内字节偏移位）
  val aligned = ioPCU.pc & ~((Config.Inst.Width / 8 - 1).U(Config.Addr.Width.W))

  // 主字缓冲（当前对齐字）
  val bufA      = RegInit(0.U(Config.Inst.Width.W))
  val bufAAddr  = RegInit(0.U(Config.Addr.Width.W))
  val bufAValid = RegInit(false.B)
  // 跨字第二字缓冲（aligned + 一字，仅 32 位指令半字对齐时使用）
  val bufB      = RegInit(0.U(Config.Inst.Width.W))
  val bufBAddr  = RegInit(0.U(Config.Addr.Width.W))
  val bufBValid = RegInit(false.B)

  val haveA    = bufAValid && (bufAAddr === aligned)
  val lowHalf  = Mux(ioPCU.pc(1), bufA(Config.Inst.Width - 1, Config.Inst.HalfWidth), bufA(Config.Inst.HalfWidth - 1, 0))
  val isComp   = lowHalf(1, 0) =/= "b11".U // 16 位压缩指令
  val crossing = ioPCU.pc(1) && !isComp    // 32 位指令跨字
  val haveB    = bufBValid && (bufBAddr === (aligned + (Config.Inst.Width / 8).U))

  val wantFetch = ioValid && !ioCtrl.pipe.flush
  val needA     = wantFetch && !haveA
  val needB     = wantFetch && haveA && crossing && !haveB

  ioIMem.valid := needA || needB
  ioIMem.pc    := Mux(needA, aligned, aligned + (Config.Inst.Width / 8).U)

  when(ioIMem.ready && needA) {
    bufA      := ioIMem.inst
    bufAAddr  := aligned
    bufAValid := true.B
    bufBValid := false.B
  }.elsewhen(ioIMem.ready && needB) {
    bufB      := ioIMem.inst
    bufBAddr  := aligned + (Config.Inst.Width / 8).U
    bufBValid := true.B
  }

  when(ioCtrl.pipe.flush) {
    bufAValid := false.B
    bufBValid := false.B
  }

  // 组装指令：低半字为 PC 处半字，高半字为 PC+半字 处半字
  val inst32 = Mux(
    isComp,
    Cat(Fill(Config.Inst.HalfWidth, 0.U), lowHalf),
    Mux(ioPCU.pc(1), Cat(bufB(Config.Inst.HalfWidth - 1, 0), bufA(Config.Inst.Width - 1, Config.Inst.HalfWidth)), bufA)
  )
  val pcInc = Mux(isComp, (Config.Inst.HalfWidth / 8).U(Config.Addr.Width.W), (Config.Inst.Width / 8).U(Config.Addr.Width.W))
  val ready = ioValid && haveA && (!crossing || haveB)

  ioIFU.pc    := ioPCU.pc
  ioIFU.inst  := inst32
  ioIFU.pcInc := pcInc

  // 顺序 PC 信息交由 Forwarding 统一处理
  ioIFUForwarding.pc    := ioPCU.pc
  ioIFUForwarding.pcInc := pcInc
  ioIFUForwarding.ready := ready

  ioCtrl.pipe.valid := ready
  ioCtrl.busy       := ioIMem.busy
  ioCtrl.stallReq   := ioValid && !ready
}