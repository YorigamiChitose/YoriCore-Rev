package Tools

import chisel3._

import Core.Bus._
import Core.Config.Config
import Core.Device._

class PlicUnitTop extends Module {
  val io = IO(new Bundle {
    val axi     = Flipped(new AXI4Master(Config.Bus.AxiDataWidth))
    val sources = Input(UInt(32.W))
    val irq     = Output(new PlicIrq)
  })
  val ctrl = Module(new DeviceController(Seq(DeviceRegion(Config.Device.PlicBase, Config.Device.PlicSize))))
  val plic = Module(new Plic)
  ctrl.io.axi <> io.axi
  ctrl.io.devices(0) <> plic.io.bus
  plic.io.sources := io.sources
  io.irq := plic.io.irq
}
