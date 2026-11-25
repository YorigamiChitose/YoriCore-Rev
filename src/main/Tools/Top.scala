package Tools

import chisel3._
import chisel3.util._

import Core._
import Core.Config.Config
import Core.Bus._
import Core.Device._

// SoC 边界：Core（内含 L1 I$/D$ + 仲裁）对外一个 MemPort，
// 由本层 AXI4Adapter 转换为标准 AXI4 主端口，再经 AXI4Interconnect
// 分流到外部存储与 DeviceController。
class Top extends Module {
  val core       = Module(new Core)
  val axiAdapter = Module(new AXI4Adapter)

  val deviceRegions = Config.Device.Regions.map { case (base, size) =>
    DeviceRegion(base, size)
  }

  val interconnect     = Module(new AXI4Interconnect(deviceRegions))
  val deviceController = Module(new DeviceController(deviceRegions))
  val irqSourceMap = Module(new IrqSourceMap)
  val syscon = Module(new Syscon)
  val clint  = Module(new Clint)
  val rtc    = Module(new Rtc)
  val uart   = Module(new Uart)
  val plic   = Module(new Plic)
  val halt   = Module(new Halt)

  core.ioMemPort <> axiAdapter.io.mem
  interconnect.io.master <> axiAdapter.io.axi
  interconnect.io.dev <> deviceController.io.axi
  deviceController.io.devices(0) <> syscon.io.bus
  deviceController.io.devices(1) <> clint.io.bus
  deviceController.io.devices(2) <> rtc.io.bus
  deviceController.io.devices(3) <> uart.io.bus
  deviceController.io.devices(4) <> plic.io.bus
  deviceController.io.devices(5) <> halt.io.bus

  val coreIrq = Wire(new CoreIrq)
  coreIrq.msi := clint.io.irq.msi
  coreIrq.mti := clint.io.irq.mti
  coreIrq.mei := plic.io.irq.ctx0
  core.ioIrq := coreIrq

  val ioAxi = IO(new AXI4Master(Config.Bus.AxiDataWidth))
  ioAxi <> interconnect.io.mem

  val ioRtcTime = IO(Input(UInt(64.W)))
  rtc.io.time := ioRtcTime

  val ioPlicSources = IO(Input(UInt(Config.Device.PlicSourceCount.W)))
  irqSourceMap.io.external := ioPlicSources
  irqSourceMap.io.clintMsi := clint.io.irq.msi
  irqSourceMap.io.uartIrq  := uart.io.irq
  plic.io.sources := irqSourceMap.io.sources
  val ioPlicIrq = IO(Output(new PlicIrq))
  ioPlicIrq := plic.io.irq

  val ioClintIrq = IO(Output(new ClintIrq))
  ioClintIrq := clint.io.irq

  val ioSyscon = IO(Output(new SysconEvent))
  ioSyscon := syscon.io.exit

  val ioUart = IO(Output(new UartTx))
  ioUart := uart.io.tx

  val ioHalt = IO(Output(new HaltEvent))
  ioHalt := halt.io.halt

  val obsTap = if (Config.Sim.Enable) Some(IO(new CoreSimTapBundle)) else None
  obsTap.foreach { s => s := core.obsTap.get }
}
