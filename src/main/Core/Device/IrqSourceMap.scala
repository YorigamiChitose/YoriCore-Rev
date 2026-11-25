package Core.Device

import chisel3._
import chisel3.util._

import Core.Config.Config

object IrqSource {
  val ClintMsi = 1
  val Uart     = 2
}

class IrqSourceMap extends Module {
  val io = IO(new Bundle {
    val external = Input(UInt(Config.Device.PlicSourceCount.W))
    val clintMsi = Input(Bool())
    val uartIrq  = Input(Bool())
    val sources  = Output(UInt(Config.Device.PlicSourceCount.W))
  })

  io.sources := io.external |
    (io.clintMsi << IrqSource.ClintMsi.U) |
    (io.uartIrq << IrqSource.Uart.U)
}
