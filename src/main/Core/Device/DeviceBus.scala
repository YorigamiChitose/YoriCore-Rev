package Core.Device

import chisel3._
import chisel3.util._

import Core.Bus._

case class DeviceRegion(base: Long, size: Long) {
  require(size > 0)
  def contains(addr: UInt): Bool = addr >= base.U && addr < (base + size).U
}

class DeviceReq(val dataWidth: Int) extends Bundle {
  val addr  = UInt(AXI4.AddrWidth.W)
  val write = Bool()
  val size  = UInt(3.W)
  val wdata = UInt(dataWidth.W)
  val wstrb = UInt((dataWidth / 8).W)
}

class DeviceResp(val dataWidth: Int) extends Bundle {
  val rdata = UInt(dataWidth.W)
  val err   = Bool()
}

class DeviceBus(val dataWidth: Int) extends Bundle {
  val req  = Decoupled(new DeviceReq(dataWidth))
  val resp = Flipped(Decoupled(new DeviceResp(dataWidth)))
}

class AXI4Slave(val dataWidth: Int) extends Bundle {
  val aw = Flipped(Decoupled(new AXI4AW))
  val w  = Flipped(Decoupled(new AXI4W(dataWidth)))
  val b  = Decoupled(new AXI4B)
  val ar = Flipped(Decoupled(new AXI4AR))
  val r  = Decoupled(new AXI4R(dataWidth))
}