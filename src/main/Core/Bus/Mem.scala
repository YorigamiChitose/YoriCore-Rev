package Core.Bus

import chisel3._
import chisel3.util._
import Core.Config.Config

// 核内自定义访存请求（主→从）
// 无 burst、无 AXI 语义；size=11 预留给缓存行访问（Phase 2 使用）
class MemReqBundle extends Bundle {
  val addr  = UInt(Config.Bus.AxiAddrWidth.W)
  val write = Bool()                                // false=读 true=写
  val size  = UInt(2.W)                             // 00=字节 01=半字 10=字 11=行(预留)
  val wdata = UInt(Config.Bus.MemDataWidth.W)       // 右对齐写数据
  val wstrb = UInt((Config.Bus.MemDataWidth / 8).W) // 右对齐字节使能
}

// 核内自定义访存响应（从→主）
class MemRespBundle extends Bundle {
  val rdata = UInt(Config.Bus.MemDataWidth.W) // 右对齐读数据（符号扩展归消费者）
  val err   = Bool()                          // 总线错误 / 非法访问
}

// 主端口视角：req 出、resp 入
class MemPort extends Bundle {
  val req  = Decoupled(new MemReqBundle)
  val resp = Flipped(Decoupled(new MemRespBundle))
}
