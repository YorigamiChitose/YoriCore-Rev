# RV32I 加载指令测试（小端）
# 覆盖：LW LB LBU LH LHU（符号/零扩展）
.section .text
.globl _start
_start:
  li t0, 0x80001000      # 数据区
  li t1, 0x12345678
  sw t1, 0(t0)           # 内存[0..3] = 78 56 34 12
  li t1, 0xFFFFFF80
  sw t1, 4(t0)           # 内存[4..7] = 80 FF FF FF

  # LW
  lw t2, 0(t0)
  li t3, 0x12345678
  bne t2, t3, fail1

  # LB 符号扩展（0x78 正数 -> 0x00000078）
  lb t2, 0(t0)
  li t3, 0x78
  bne t2, t3, fail2

  # LB 符号扩展（0x80 负数 -> 0xFFFFFF80）
  lb t2, 4(t0)
  li t3, 0xFFFFFF80
  bne t2, t3, fail3

  # LBU 零扩展（0x80 -> 0x00000080）
  lbu t2, 4(t0)
  li t3, 0x80
  bne t2, t3, fail4

  # LH 符号扩展（0x5678 正数 -> 0x00005678）
  lh t2, 0(t0)
  li t3, 0x5678
  bne t2, t3, fail5

  # LH 符号扩展（0xFF80 负数 -> 0xFFFFFF80）
  lh t2, 4(t0)
  li t3, 0xFFFFFF80
  bne t2, t3, fail6

  # LHU 零扩展（0xFF80 -> 0x0000FF80）
  lhu t2, 4(t0)
  li t3, 0xFF80
  bne t2, t3, fail7

  li a0, 0
  ebreak

fail1: li a0, 1; ebreak
fail2: li a0, 2; ebreak
fail3: li a0, 3; ebreak
fail4: li a0, 4; ebreak
fail5: li a0, 5; ebreak
fail6: li a0, 6; ebreak
fail7: li a0, 7; ebreak
