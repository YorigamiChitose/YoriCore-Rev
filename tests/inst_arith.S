# RV32I 寄存器-寄存器算术/逻辑指令测试
# 覆盖：ADD SUB SLL SRL SRA SLT SLTU XOR OR AND
.section .text
.globl _start
_start:
  # ADD
  li t0, 3
  li t1, 4
  add t2, t0, t1          # 7
  li t3, 7
  bne t2, t3, fail1

  # SUB
  li t0, 10
  li t1, 6
  sub t2, t0, t1          # 4
  li t3, 4
  bne t2, t3, fail2

  # SUB 负数回绕
  li t0, 3
  li t1, 5
  sub t2, t0, t1          # -2 = 0xFFFFFFFE
  li t3, -2
  bne t2, t3, fail3

  # SLL
  li t0, 1
  li t1, 5
  sll t2, t0, t1          # 32
  li t3, 32
  bne t2, t3, fail4

  # SLL 移位量只取低 5 位（1 << (36 mod 32) = 1 << 4 = 16）
  li t0, 1
  li t1, 36
  sll t2, t0, t1
  li t3, 16
  bne t2, t3, fail5

  # SRL 逻辑右移补 0
  li t0, 0x80000000
  li t1, 4
  srl t2, t0, t1          # 0x08000000
  li t3, 0x08000000
  bne t2, t3, fail6

  # SRA 算术右移（负数补 1）
  li t0, 0x80000000
  li t1, 4
  sra t2, t0, t1          # 0xF8000000
  li t3, 0xF8000000
  bne t2, t3, fail7

  # SLT 有符号：-1 < 1 -> 1
  li t0, -1
  li t1, 1
  slt t2, t0, t1
  li t3, 1
  bne t2, t3, fail8

  # SLT 有符号：1 < -1 -> 0
  li t0, 1
  li t1, -1
  slt t2, t0, t1
  bne t2, x0, fail9

  # SLTU 无符号：1 < 0xFFFFFFFF -> 1
  li t0, 1
  li t1, -1
  sltu t2, t0, t1
  li t3, 1
  bne t2, t3, fail10

  # XOR
  li t0, 0x0F0F
  li t1, 0x00FF
  xor t2, t0, t1          # 0x0FF0
  li t3, 0x0FF0
  bne t2, t3, fail11

  # OR
  li t0, 0x0F00
  li t1, 0x00FF
  or t2, t0, t1           # 0x0FFF
  li t3, 0x0FFF
  bne t2, t3, fail12

  # AND
  li t0, 0x0F0F
  li t1, 0xFF0F
  and t2, t0, t1          # 0x0F0F
  li t3, 0x0F0F
  bne t2, t3, fail13

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
fail12: li a0, 12; ebreak
fail13: li a0, 13; ebreak
