package Core.Bus

import chisel3._
import chisel3.util._

import Core.Config.Config
import Core.Device.DeviceRegion

class AXI4Interconnect(val devRegions: Seq[DeviceRegion], val dataWidth: Int = Config.Bus.AxiDataWidth) extends Module {
  val io = IO(new Bundle {
    val master = Flipped(new AXI4Master(dataWidth))
    val mem    = new AXI4Master(dataWidth)
    val dev    = new AXI4Master(dataWidth)
  })

  val sIdle :: sW :: sB :: sR :: Nil = Enum(4)
  val state                         = RegInit(sIdle)

  val sel  = RegInit(false.B)
  val selR = RegInit(false.B)

  val devHit  = devRegions.foldLeft(false.B) { case (acc, r) => acc || r.contains(io.master.aw.bits.addr) }
  val devHitR = devRegions.foldLeft(false.B) { case (acc, r) => acc || r.contains(io.master.ar.bits.addr) }
  val memHit  = !devHit
  val memHitR = !devHitR

  io.master.aw.ready := false.B
  io.master.w.ready  := false.B
  io.master.b.valid  := false.B
  io.master.b.bits   := 0.U.asTypeOf(new AXI4B)
  io.master.ar.ready := false.B
  io.master.r.valid  := false.B
  io.master.r.bits   := 0.U.asTypeOf(new AXI4R(dataWidth))

  io.mem.aw.valid := false.B
  io.mem.aw.bits  := 0.U.asTypeOf(new AXI4AW)
  io.mem.w.valid  := false.B
  io.mem.w.bits   := 0.U.asTypeOf(new AXI4W(dataWidth))
  io.mem.b.ready  := false.B
  io.mem.ar.valid := false.B
  io.mem.ar.bits  := 0.U.asTypeOf(new AXI4AR)
  io.mem.r.ready  := false.B

  io.dev.aw.valid := false.B
  io.dev.aw.bits  := 0.U.asTypeOf(new AXI4AW)
  io.dev.w.valid  := false.B
  io.dev.w.bits   := 0.U.asTypeOf(new AXI4W(dataWidth))
  io.dev.b.ready  := false.B
  io.dev.ar.valid := false.B
  io.dev.ar.bits  := 0.U.asTypeOf(new AXI4AR)
  io.dev.r.ready  := false.B

  when(state === sIdle) {
    io.master.aw.ready := Mux(memHit, io.mem.aw.ready, io.dev.aw.ready)
    io.mem.aw.valid    := io.master.aw.valid && memHit
    io.dev.aw.valid    := io.master.aw.valid && !memHit
    io.mem.aw.bits     := io.master.aw.bits
    io.dev.aw.bits     := io.master.aw.bits

    io.master.ar.ready := !io.master.aw.valid && Mux(memHitR, io.mem.ar.ready, io.dev.ar.ready)
    io.mem.ar.valid    := io.master.ar.valid && memHitR
    io.dev.ar.valid    := io.master.ar.valid && !memHitR
    io.mem.ar.bits     := io.master.ar.bits
    io.dev.ar.bits     := io.master.ar.bits

    when(io.master.aw.fire) {
      sel   := !memHit
      state := sW
    }.elsewhen(io.master.ar.fire) {
      selR  := !memHitR
      state := sR
    }
  }

  when(state === sW) {
    io.master.w.ready := Mux(sel, io.dev.w.ready, io.mem.w.ready)
    io.mem.w.valid    := io.master.w.valid && !sel
    io.dev.w.valid    := io.master.w.valid && sel
    io.mem.w.bits     := io.master.w.bits
    io.dev.w.bits     := io.master.w.bits
    when(io.master.w.fire && io.master.w.bits.last) { state := sB }
  }

  when(state === sB) {
    io.mem.b.ready    := !sel && io.master.b.ready
    io.dev.b.ready    := sel && io.master.b.ready
    io.master.b.valid := Mux(sel, io.dev.b.valid, io.mem.b.valid)
    io.master.b.bits  := Mux(sel, io.dev.b.bits, io.mem.b.bits)
    when(io.master.b.fire) { state := sIdle }
  }

  when(state === sR) {
    io.mem.r.ready    := !selR && io.master.r.ready
    io.dev.r.ready    := selR && io.master.r.ready
    io.master.r.valid := Mux(selR, io.dev.r.valid, io.mem.r.valid)
    io.master.r.bits  := Mux(selR, io.dev.r.bits, io.mem.r.bits)
    when(io.master.r.fire && io.master.r.bits.last) { state := sIdle }
  }
}