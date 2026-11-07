package Core.IFU

import chisel3._
import chisel3.util._

import Core.Config.Config
import Core.IFU.module._
import Core.PCU.module._
import Core.Pipe.module._
import Core.Bus._

// IFU：支持 RV32C 的变长取指。
// 维护一个对齐字缓冲 + 跨字第二字缓冲，按 16/32 位组装指令并给出 pcInc。
// 取指经统一 MemPort 读字（I$）。
class IFU extends Module {
  val ioCtrl          = IO(new IFUCtrlBundle())                // 控制信号
  val ioIFU           = IO(new IFUStageBundle())               // 下级流水信号
  val ioPCU           = IO(Flipped(new PCUStageBundle()))      // 上级流水信号
  val ioMemPort       = IO(new MemPort)                        // 取指存储器（主侧，读字）
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
  val isComp   = lowHalf(1, 0) =/= "b11".U
  val crossing = ioPCU.pc(1) && !isComp
  val haveB    = bufBValid && (bufBAddr === (aligned + (Config.Inst.Width / 8).U))

  val wantFetch = ioValid && !ioCtrl.pipe.flush
  val needA     = wantFetch && !haveA
  val needB     = wantFetch && haveA && crossing && !haveB

  // 取指（MemPort 读字，单 outstanding）
  val fetchPending = RegInit(false.B)
  val fetchAddr    = RegInit(0.U(Config.Addr.Width.W))
  val fetchIsB     = RegInit(false.B)

  val issue = (needA || needB) && !fetchPending
  ioMemPort.req.valid      := issue
  ioMemPort.req.bits.addr  := Mux(needA, aligned, aligned + (Config.Inst.Width / 8).U)
  ioMemPort.req.bits.write := false.B
  ioMemPort.req.bits.size  := MemSize.W // 字
  ioMemPort.req.bits.wdata := 0.U
  ioMemPort.req.bits.wstrb := 0.U
  ioMemPort.req.bits.amoOp := AmoOp.NOP

  when(ioMemPort.req.fire) {
    fetchPending := true.B
    fetchAddr    := Mux(needA, aligned, aligned + (Config.Inst.Width / 8).U)
    fetchIsB     := needB
  }

  ioMemPort.resp.ready := fetchPending
  when(ioMemPort.resp.fire) {
    fetchPending := false.B
    when(fetchIsB) {
      bufB      := ioMemPort.resp.bits.rdata(Config.Inst.Width - 1, 0)
      bufBAddr  := fetchAddr
      bufBValid := true.B
    }.otherwise {
      bufA      := ioMemPort.resp.bits.rdata(Config.Inst.Width - 1, 0)
      bufAAddr  := fetchAddr
      bufAValid := true.B
      bufBValid := false.B
    }
  }

  when(ioCtrl.pipe.flush) {
    fetchPending := false.B
    bufAValid    := false.B
    bufBValid    := false.B
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
  ioCtrl.busy       := fetchPending
  ioCtrl.stallReq   := ioValid && !ready
}