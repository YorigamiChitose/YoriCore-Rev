package Tools

import chisel3._

import Core.Bus._
import Core.Config.Config
import Core.Device._

class UartUnitTop extends Module {
  val io = IO(Flipped(new AXI4Master(Config.Bus.AxiDataWidth)))
  val ctrl = Module(new DeviceController(Seq(DeviceRegion(Config.Device.UartBase, Config.Device.UartSize))))
  val uart = Module(new Uart)
  ctrl.io.axi <> io
  ctrl.io.devices(0) <> uart.io.bus
  val tx = IO(Output(new UartTx))
  tx := uart.io.tx
}
