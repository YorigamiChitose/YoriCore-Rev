package Tools

import chisel3._
import chisel3.util._

import Core.Bus._
import Core.Config.Config
import Core.Device._

class DeviceUnitDevice extends Module {
  val io = IO(Flipped(new DeviceBus(Config.Bus.AxiDataWidth)))

  val pending = RegInit(false.B)
  val saved   = RegInit(0.U(Config.Bus.AxiDataWidth.W))

  io.req.ready  := !pending
  io.resp.valid := pending
  io.resp.bits.rdata := saved
  io.resp.bits.err   := false.B

  when(io.req.fire) {
    pending := true.B
    saved := Mux(io.req.bits.write, io.req.bits.wdata, saved)
  }
  when(io.resp.fire) {
    pending := false.B
  }
}

class DeviceUnitTop extends Module {
  val io = IO(Flipped(new AXI4Master(Config.Bus.AxiDataWidth)))

  val ctrl = Module(
    new DeviceController(
      Seq(
        DeviceRegion(0x0000_1000L, 0x100L),
        DeviceRegion(0x0000_2000L, 0x100L)
      ),
      Config.Bus.AxiDataWidth
    )
  )
  val dev0 = Module(new DeviceUnitDevice)
  val dev1 = Module(new DeviceUnitDevice)

  ctrl.io.axi <> io
  ctrl.io.devices(0) <> dev0.io
  ctrl.io.devices(1) <> dev1.io
}