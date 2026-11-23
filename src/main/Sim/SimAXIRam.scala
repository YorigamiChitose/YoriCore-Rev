package Sim

import chisel3._
import chisel3.util._

import Core.Bus._
import Core.Config.Config

// AXI4 从设备存储模型黑盒（对应 resources/SimAXIRam.sv），带可配置响应延迟
class SimAXIRam(latency: Int) extends BlackBox(Map("LATENCY" -> latency)) {
  override def desiredName = "SimAXIRam"
  val io                   = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Reset())
    val axi   = Flipped(new AXI4Master(Config.Bus.AxiDataWidth))
  })
}
