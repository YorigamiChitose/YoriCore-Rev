package Core.Config

import chisel3._
import chisel3.util._

object Config {
  object Inst {
    val Width     = 32
    val HalfWidth = Width / 2 // 16 位压缩指令半字宽度
  }

  object Addr {
    val Base  = 0x8000_0000L
    val Width = 32
  }

  object Reg {
    val Num      = 32
    val Width    = Data.XLEN
    val NumWidth = chisel3.util.log2Ceil(Num)
  }

  object Csr {
    val DataWidth = Data.XLEN
    val AddrWidth = 12
  }

  object Data {
    val XLEN = 32
    val B    = 8
    val H    = 2 * B
    val W    = 2 * H
    val D    = 2 * W
  }

  object Bus {
    val AxiAddrWidth = Addr.Width // 物理地址宽度
    val AxiDataWidth = 64         // 统一 AXI 数据位宽（行突发 4 拍 / 字窄传输单拍）
    val AxiIdWidth   = 1
  }

  object Mmio {
    val Base = 0xF000_0000L // MMIO 起始地址（>= Base 为不可缓存，直通）
  }

  object Sim {
    val Enable = true // 仿真可观测层总开关（SMU/SimInfo 注入）
  }

  object Cache {
    val LineBytes  = 32                  // 缓存行大小（字节）
    val LineBits   = LineBytes * 8       // 缓存行位宽 256
    val LineWidth  = log2Ceil(LineBytes) // 行内字节偏移位 5

    val NumWays    = 4                   // 组相联路数
    val NumSets    = 64                  // 组数
    val PLRUBits   = NumWays - 1         // 树形 PLRU 每组位数
    val IndexWidth = log2Ceil(NumSets)   // 组索引位宽 6
    val TagWidth   = Addr.Width - IndexWidth - LineWidth // tag 位宽
    val WordBits   = log2Ceil(LineBits / Data.XLEN)      // 行内字数位宽
  }
}
