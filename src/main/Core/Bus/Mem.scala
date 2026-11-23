package Core.Bus

import chisel3._
import chisel3.util._

import Core.Config.Config

// 原子操作编码（下沉到缓存内 RMW）
object AmoOp {
  val NOP  = 0.U(4.W)
  val LR   = 1.U(4.W)
  val SC   = 2.U(4.W)
  val SWAP = 3.U(4.W)
  val ADD  = 4.U(4.W)
  val XOR  = 5.U(4.W)
  val AND  = 6.U(4.W)
  val OR   = 7.U(4.W)
  val MIN  = 8.U(4.W)
  val MAX  = 9.U(4.W)
  val MINU = 10.U(4.W)
  val MAXU = 11.U(4.W)
}

// 访存粒度统一编码（字/半字/字节用低 32 位，行用全 256 位）
object MemSize {
  val B    = 0.U(2.W)
  val H    = 1.U(2.W)
  val W    = 2.U(2.W)
  val Line = 3.U(2.W)
}

// 统一访存请求：取指/load/store/AMO/行填充/行写回/MMIO 直通 全部共用这一种请求
class MemReqBundle extends Bundle {
  val addr  = UInt(Config.Addr.Width.W)
  val write = Bool()                              // false=读 true=写
  val size  = UInt(2.W)                           // MemSize.*
  val wdata = UInt(Config.Cache.LineBits.W)       // 统一行宽；字/字节/半字用低 32 位
  val wstrb = UInt((Config.Cache.LineBits / 8).W) // 统一字节使能；字/字节/半字用低 4 位
  val amoOp = UInt(4.W)                           // AmoOp.*
}

// 统一访存响应：行访问 rdata 全 256 位；字/字节/半字 rdata(31,0) 为右对齐结果
class MemRespBundle extends Bundle {
  val rdata = UInt(Config.Cache.LineBits.W)
  val err   = Bool()
}

class MemPort extends Bundle {
  val req  = Decoupled(new MemReqBundle)
  val resp = Flipped(Decoupled(new MemRespBundle))
}
