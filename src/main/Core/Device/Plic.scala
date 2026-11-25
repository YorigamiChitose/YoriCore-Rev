package Core.Device

import chisel3._
import chisel3.util._

import Core.Config.Config

class PlicIrq extends Bundle {
  val ctx0 = Bool()
  val ctx1 = Bool()
}

class Plic(val numSources: Int = 32, val dataWidth: Int = Config.Bus.AxiDataWidth) extends Module {
  require(numSources > 1 && numSources <= 32)

  val io = IO(new Bundle {
    val bus     = Flipped(new DeviceBus(dataWidth))
    val sources = Input(UInt(numSources.W))
    val irq     = Output(new PlicIrq)
  })

  val pending   = RegInit(0.U(numSources.W))
  val priority  = RegInit(VecInit(Seq.fill(numSources)(0.U(32.W))))
  val enable    = RegInit(VecInit(Seq.fill(2)(0.U(numSources.W))))
  val threshold = RegInit(VecInit(Seq.fill(2)(0.U(32.W))))
  val rdata     = RegInit(0.U(dataWidth.W))
  val rerr      = RegInit(false.B)
  val busy      = RegInit(false.B)

  val off = io.bus.req.bits.addr(21, 0)

  val prioHit  = off < 0x1000.U
  val pendHit  = off >= 0x1000.U && off < 0x1004.U
  val enHit0   = off >= 0x2000.U && off < 0x2004.U
  val enHit1   = off >= 0x2080.U && off < 0x2084.U
  val thHit0   = off >= 0x200000.U && off < 0x200004.U
  val thHit1   = off >= 0x201000.U && off < 0x201004.U
  val claimHit0 = off >= 0x200004.U && off < 0x200008.U
  val claimHit1 = off >= 0x201004.U && off < 0x201008.U
  val hit      = prioHit || pendHit || enHit0 || enHit1 || thHit0 || thHit1 || claimHit0 || claimHit1

  val srcIdx = off(6, 2)

  def bestOf(ctx: Int): UInt = {
    val cand = Wire(Vec(numSources, Bool()))
    for (i <- 0 until numSources) {
      cand(i) := pending(i) && enable(ctx)(i) && priority(i) > threshold(ctx)
    }
    PriorityEncoder(cand)
  }

  val best0 = bestOf(0)
  val best1 = bestOf(1)
  val active0 = Wire(Vec(numSources, Bool()))
  val active1 = Wire(Vec(numSources, Bool()))
  for (i <- 0 until numSources) {
    active0(i) := pending(i) && enable(0)(i) && priority(i) > threshold(0)
    active1(i) := pending(i) && enable(1)(i) && priority(i) > threshold(1)
  }
  io.irq.ctx0 := active0.asUInt.orR
  io.irq.ctx1 := active1.asUInt.orR

  io.bus.req.ready      := !busy
  io.bus.resp.valid     := busy
  io.bus.resp.bits.rdata := rdata
  io.bus.resp.bits.err   := rerr

  pending := pending | (io.sources & ~1.U(numSources.W))

  when(io.bus.req.fire) {
    busy  := true.B
    rerr  := !hit
    rdata := 0.U
    when(prioHit && srcIdx < numSources.U) { rdata := Cat(0.U(32.W), priority(srcIdx)) }
    when(pendHit) { rdata := Cat(0.U((dataWidth - numSources).W), pending) }
    when(enHit0) { rdata := Cat(0.U((dataWidth - numSources).W), enable(0)) }
    when(enHit1) { rdata := Cat(0.U((dataWidth - numSources).W), enable(1)) }
    when(thHit0) { rdata := Cat(0.U(32.W), threshold(0)) }
    when(thHit1) { rdata := Cat(0.U(32.W), threshold(1)) }
    when(claimHit0) {
      rdata := Cat(0.U(32.W), best0)
      pending := pending & ~(1.U(numSources.W) << best0)
    }
    when(claimHit1) {
      rdata := Cat(0.U(32.W), best1)
      pending := pending & ~(1.U(numSources.W) << best1)
    }
    when(io.bus.req.bits.write) {
      when(prioHit && srcIdx < numSources.U) { priority(srcIdx) := io.bus.req.bits.wdata(31, 0) }
      when(enHit0) { enable(0) := io.bus.req.bits.wdata(numSources - 1, 0) }
      when(enHit1) { enable(1) := io.bus.req.bits.wdata(numSources - 1, 0) }
      when(thHit0) { threshold(0) := io.bus.req.bits.wdata(31, 0) }
      when(thHit1) { threshold(1) := io.bus.req.bits.wdata(31, 0) }
      when(claimHit0 && io.bus.req.bits.wdata(31, 0) =/= 0.U) { pending := pending }
      when(claimHit1 && io.bus.req.bits.wdata(31, 0) =/= 0.U) { pending := pending }
    }
  }

  when(io.bus.resp.fire) {
    busy := false.B
    rerr := false.B
  }
}
