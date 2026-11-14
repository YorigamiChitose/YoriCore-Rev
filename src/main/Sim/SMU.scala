package Sim

import chisel3._
import chisel3.util._

import Core.Config.Config
import Core.IDU.module._

// SMU（Simulation Monitor Unit）：提交流监控。
// 接收 WBU 实际提交的指令信息，仅做观测，不驱动核心。
class SMU extends Module {
  val io = IO(new Bundle {
    val commitValid = Input(Bool())                       // 本拍提交
    val commitPc    = Input(UInt(Config.Addr.Width.W))    // 提交 PC
    val commitExc   = Input(exc())                        // 异常类型
    val rdEn        = Input(Bool())                       // 写 rd
    val rdAddr      = Input(UInt(Config.Reg.NumWidth.W))  // rd 地址
    val rdData      = Input(UInt(Config.Reg.Width.W))     // rd 数据
    val csrEn       = Input(Bool())                       // 写 csr
    val csrAddr     = Input(UInt(Config.Csr.AddrWidth.W)) // csr 地址
    val csrData     = Input(UInt(Config.Csr.DataWidth.W)) // csr 数据

    val ebreak = Output(Bool()) // 检测到 ebreak 提交
  })

  io.ebreak := io.commitValid && (io.commitExc === exc.EBREAK)
}