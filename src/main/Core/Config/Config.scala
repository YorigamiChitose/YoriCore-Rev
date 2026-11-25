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

  object Mem {
    // 可缓存 DRAM 区间：与仿真 autoconf.h 的 CONFIG_MBASE/CONFIG_MSIZE 对齐
    val Base = 0x8000_0000L // 内存基址（= Addr.Base = 复位向量）
    val Size = 0x1000_0000L // 16MB
  }

  object Device {
    val SysconBase = 0x0010_0000L
    val SysconSize = 0x1000L
    val PlicBase  = 0x0c00_0000L
    val PlicSize  = 0x0040_0000L
    val ClintBase = 0x0200_0000L
    val ClintSize = 0xc000L
    val RtcBase  = 0x0010_1000L
    val RtcSize  = 0x1000L
    val UartBase = 0x1000_0000L
    val UartSize = 0x1000L
    val HaltBase = 0xf000_0000L
    val HaltSize = 0x1000L

    val Regions: Seq[(Long, Long)] = Seq(
      (SysconBase, SysconSize),
      (ClintBase, ClintSize),
      (RtcBase, RtcSize),
      (UartBase, UartSize),
      (PlicBase, PlicSize),
      (HaltBase, HaltSize)
    )
  }

  // 物理内存属性（PMA）译码：cache 与 mem 共用的完备地址区间检查。
  // 任一地址必须显式归类为「内存（可缓存）」「MMIO 设备（不可缓存直通）」「未映射」，
  // 不允许靠排除法省略对设备区间的判断。
  object Pma {
    // 是否落在 [base, base+size) 区间
    private def inRange(addr: UInt, base: Long, size: Long): Bool =
      addr >= base.U && addr < (base + size).U

    // 是否落在可缓存内存区间
    def isMem(addr: UInt): Bool = inRange(addr, Mem.Base, Mem.Size)

    // 是否落在任一 MMIO 设备区间（显式检查每个设备，非空安全）
    def isMmio(addr: UInt): Bool =
      Device.Regions.foldLeft(false.B) { case (acc, (base, size)) =>
        acc || inRange(addr, base, size)
      }

    // 是否可缓存：必须在内存区间内，且不属于任何 MMIO 设备区间（完备检查）
    def isCacheable(addr: UInt): Bool = isMem(addr) && !isMmio(addr)
  }

  object Sim {
    val Enable     = true // 仿真可观测层总开关（SMU/SimInfo/tap/观测字段注入）
    val AxiLatency = 0    // AXI 存储模型响应延迟（额外等待周期）

    // 仿真观测字段宽度：关闭时取 0 宽，firtool 会完全消除，保证不影响内核
    def obsWidth(w: Int): Int  = if (Enable) w else 0
    def obsUInt(w:  Int): UInt = UInt(obsWidth(w).W)
  }

  object Cache {
    val LineBytes = 32                  // 缓存行大小（字节）
    val LineBits  = LineBytes * 8       // 缓存行位宽 256
    val LineWidth = log2Ceil(LineBytes) // 行内字节偏移位 5

    val NumWays    = 4                                   // 组相联路数
    val NumSets    = 64                                  // 组数
    val PLRUBits   = NumWays - 1                         // 树形 PLRU 每组位数
    val IndexWidth = log2Ceil(NumSets)                   // 组索引位宽 6
    val TagWidth   = Addr.Width - IndexWidth - LineWidth // tag 位宽
    val WordBits   = log2Ceil(LineBits / Data.XLEN)      // 行内字数位宽
  }
}
