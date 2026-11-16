# RV32I CSR 指令测试
# 覆盖：CSRRW CSRRS CSRRC CSRRWI CSRRSI CSRRCI（使用 mcause）
.section .text
.globl _start
_start:
  # CSRRW：写 mcause=0x123，读回旧值到 t1（初值 0）
  li t0, 0x123
  csrrw t1, mcause, t0
  csrr t2, mcause
  li t3, 0x123
  bne t2, t3, fail1
  # 旧值应为 0
  bne t1, x0, fail2

  # CSRRWI：立即数写 mcause=5，读回旧值 0x123 到 t1
  csrrwi t1, mcause, 5
  csrr t2, mcause
  li t3, 5
  bne t2, t3, fail3
  # 旧值应为 0x123
  li t3, 0x123
  bne t1, t3, fail4

  # CSRRS：置位（mcause 0 -> 0x0F）
  csrw mcause, x0
  li t0, 0x0F
  csrrs t1, mcause, t0
  csrr t2, mcause
  li t3, 0x0F
  bne t2, t3, fail5

  # CSRRC：清零位（0x0F & ~0x03 = 0x0C）
  li t0, 0x03
  csrrc t1, mcause, t0
  csrr t2, mcause
  li t3, 0x0C
  bne t2, t3, fail6

  # CSRRSI：立即数置位（0x0C | 0x01 = 0x0D）
  csrrsi t1, mcause, 1
  csrr t2, mcause
  li t3, 0x0D
  bne t2, t3, fail7

  # CSRRCI：立即数清零（0x0D & ~0x01 = 0x0C）
  csrrci t1, mcause, 1
  csrr t2, mcause
  li t3, 0x0C
  bne t2, t3, fail8

  li a0, 0
  ebreak

fail1: li a0, 1; ebreak
fail2: li a0, 2; ebreak
fail3: li a0, 3; ebreak
fail4: li a0, 4; ebreak
fail5: li a0, 5; ebreak
fail6: li a0, 6; ebreak
fail7: li a0, 7; ebreak
fail8: li a0, 8; ebreak
