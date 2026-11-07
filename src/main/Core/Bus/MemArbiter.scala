package Core.Bus

import chisel3._
import chisel3.util._

// 单一请求仲裁器：I$ 与 D$ 两个 MemPort 合并为一个，固定优先级（in0 > in1），
// 单 outstanding（响应按来源回送）。
class MemArbiter extends Module {
  val io = IO(new Bundle {
    val in0 = Flipped(new MemPort)
    val in1 = Flipped(new MemPort)
    val out = new MemPort
  })

  val busy = RegInit(false.B) // 是否有请求在途
  val sel  = RegInit(false.B) // 在途来源：false=in0 true=in1

  val grant0 = !busy && io.in0.req.valid
  val grant1 = !busy && !io.in0.req.valid && io.in1.req.valid

  io.in0.req.ready := grant0 && io.out.req.ready
  io.in1.req.ready := grant1 && io.out.req.ready

  io.out.req.valid := grant0 || grant1
  io.out.req.bits  := Mux(grant0, io.in0.req.bits, io.in1.req.bits)

  when(io.out.req.fire) {
    busy := true.B
    sel  := grant1
  }

  io.in0.resp.valid := busy && !sel && io.out.resp.valid
  io.in1.resp.valid := busy &&  sel && io.out.resp.valid
  io.in0.resp.bits  := io.out.resp.bits
  io.in1.resp.bits  := io.out.resp.bits
  io.out.resp.ready := Mux(sel, io.in1.resp.ready, io.in0.resp.ready)

  when(busy && io.out.resp.fire) {
    busy := false.B
  }
}