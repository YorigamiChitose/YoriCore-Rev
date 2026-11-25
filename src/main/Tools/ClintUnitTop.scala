package Tools

import chisel3._

import Core.Bus._
import Core.Config.Config
import Core.Device._

class ClintUnitTop extends Module {
  val io = IO(Flipped(new AXI4Master(Config.Bus.AxiDataWidth)))
  val ctrl = Module(new DeviceController(Seq(DeviceRegion(Config.Device.ClintBase, Config.Device.ClintSize))))
  val clint = Module(new Clint)
  ctrl.io.axi <> io
  ctrl.io.devices(0) <> clint.io.bus
  val irq = IO(Output(new ClintIrq))
  irq := clint.io.irq
}
