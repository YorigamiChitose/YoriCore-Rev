package Core.Bus

import chisel3._
import chisel3.util._
import Core.Config.Config

object AXI4 {
  val AddrWidth = Config.Bus.AxiAddrWidth
  val IdWidth   = Config.Bus.AxiIdWidth

  object Burst {
    val FIXED = 0.U(2.W)
    val INCR  = 1.U(2.W)
    val WRAP  = 2.U(2.W)
  }

  object Resp {
    val OKAY   = 0.U(2.W)
    val EXOKAY = 1.U(2.W)
    val SLVERR = 2.U(2.W)
    val DECERR = 3.U(2.W)
  }
}

// 写地址通道
class AXI4AW extends Bundle {
  val id    = UInt(AXI4.IdWidth.W)
  val addr  = UInt(AXI4.AddrWidth.W)
  val len   = UInt(8.W)
  val size  = UInt(3.W)
  val burst = UInt(2.W)
  val cache = UInt(4.W)
  val prot  = UInt(3.W)
}

class AXI4W(val dataWidth: Int) extends Bundle {
  val data = UInt(dataWidth.W)
  val strb = UInt((dataWidth / 8).W)
  val last = Bool()
}

class AXI4B extends Bundle {
  val id   = UInt(AXI4.IdWidth.W)
  val resp = UInt(2.W)
}

// 读地址通道
class AXI4AR extends Bundle {
  val id    = UInt(AXI4.IdWidth.W)
  val addr  = UInt(AXI4.AddrWidth.W)
  val len   = UInt(8.W)
  val size  = UInt(3.W)
  val burst = UInt(2.W)
  val cache = UInt(4.W)
  val prot  = UInt(3.W)
}

class AXI4R(val dataWidth: Int) extends Bundle {
  val id   = UInt(AXI4.IdWidth.W)
  val data = UInt(dataWidth.W)
  val resp = UInt(2.W)
  val last = Bool()
}

// AXI4 主端口视角（按数据位宽参数化）
class AXI4Master(val dataWidth: Int) extends Bundle {
  val aw = Decoupled(new AXI4AW)
  val w  = Decoupled(new AXI4W(dataWidth))
  val b  = Flipped(Decoupled(new AXI4B))
  val ar = Decoupled(new AXI4AR)
  val r  = Flipped(Decoupled(new AXI4R(dataWidth)))
}
