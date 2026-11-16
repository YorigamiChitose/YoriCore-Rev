# RV32I 立即数算术/逻辑指令测试
# 覆盖：ADDI SLTI SLTIU XORI ORI ANDI SLLI SRLI SRAI
.section .text
.globl _start
_start:
  # ADDI
  li t0, 100
  addi t1, t0, 23         # 123
  li t2, 123
  bne t1, t2, fail1

  # ADDI 负数立即数
  li t0, 100
  addi t1, t0, -50        # 50
  li t2, 50
  bne t1, t2, fail2

  # SLTI 有符号：-1 < 0 -> 1
  li t0, -1
  slti t1, t0, 0
  li t2, 1
  bne t1, t2, fail3

  # SLTI 有符号：5 < 3 -> 0
  li t0, 5
  slti t1, t0, 3
  bne t1, x0, fail4

  # SLTIU 无符号：1 < 0x7FF -> 1
  li t0, 1
  sltiu t1, t0, 0x7FF
  li t2, 1
  bne t1, t2, fail5

  # XORI
  li t0, 0x0F
  xori t1, t0, 0x1F       # 0x10
  li t2, 0x10
  bne t1, t2, fail6

  # ORI
  li t0, 0x0F00
  ori t1, t0, 0x0FF       # 0x0FFF
  li t2, 0x0FFF
  bne t1, t2, fail7

  # ANDI
  li t0, 0x0F0F
  andi t1, t0, 0x0FF      # 0x000F
  li t2, 0x0F
  bne t1, t2, fail8

  # SLLI
  li t0, 1
  slli t1, t0, 10         # 1024
  li t2, 1024
  bne t1, t2, fail9

  # SRLI
  li t0, 0x80000000
  srli t1, t0, 12         # 0x00080000
  li t2, 0x00080000
  bne t1, t2, fail10

  # SRAI 算术右移（负数补 1）
  li t0, 0x80000000
  srai t1, t0, 8          # 0xFF800000
  li t2, 0xFF800000
  bne t1, t2, fail11

  li a0, 0
  ebreak

fail1:  li a0, 1;  ebreak
fail2:  li a0, 2;  ebreak
fail3:  li a0, 3;  ebreak
fail4:  li a0, 4;  ebreak
fail5:  li a0, 5;  ebreak
fail6:  li a0, 6;  ebreak
fail7:  li a0, 7;  ebreak
fail8:  li a0, 8;  ebreak
fail9:  li a0, 9;  ebreak
fail10: li a0, 10; ebreak
fail11: li a0, 11; ebreak
