# RV32I 存储指令测试（小端）
# 覆盖：SW SH SB（写后读回，验证字节使能/不越界）
.section .text
.globl _start
_start:
  li t0, 0x80001000      # 数据区

  # SW
  li t1, 0xDEADBEEF
  sw t1, 0(t0)
  lw t2, 0(t0)
  bne t2, t1, fail1

  # SH：先清空再写半字，读回校验
  sw x0, 4(t0)
  li t1, 0x1234
  sh t1, 4(t0)
  lhu t2, 4(t0)
  bne t2, t1, fail2
  # 高位字节应保持 0
  lbu t2, 6(t0)
  bne t2, x0, fail3

  # SB：先清空再写单字节
  sw x0, 8(t0)
  li t1, 0xAB
  sb t1, 8(t0)
  lbu t2, 8(t0)
  bne t2, t1, fail4
  # 相邻字节应保持 0（字节使能不越界）
  lbu t2, 9(t0)
  bne t2, x0, fail5

  li a0, 0
  ebreak

fail1: li a0, 1; ebreak
fail2: li a0, 2; ebreak
fail3: li a0, 3; ebreak
fail4: li a0, 4; ebreak
fail5: li a0, 5; ebreak
