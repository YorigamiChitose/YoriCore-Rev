# RV32M 乘法指令测试
# 覆盖：MUL MULH MULHSU MULHU
.section .text
.globl _start
_start:
  # MUL 低 32 位
  li t0, 0x12345
  li t1, 0x100
  mul t2, t0, t1         # 0x1234500
  li t3, 0x1234500
  bne t2, t3, fail1

  # MUL 负数
  li t0, -3
  li t1, 5
  mul t2, t0, t1         # -15
  li t3, -15
  bne t2, t3, fail2

  # MULH 有符号高 32 位（小正数，高位为 0）
  li t0, -2
  li t1, -3
  mulh t2, t0, t1        # (-2)*(-3)=6，高 32 位 = 0
  bne t2, x0, fail3

  # MULH 溢出到高位（0x40000000*4 = 0x100000000，高 32 位 = 1）
  li t0, 0x40000000
  li t1, 4
  mulh t2, t0, t1
  li t3, 1
  bne t2, t3, fail4

  # MULHU 无符号高 32 位（0x80000000*2 = 0x100000000，高位 = 1）
  li t0, 0x80000000
  li t1, 2
  mulhu t2, t0, t1
  li t3, 1
  bne t2, t3, fail5

  # MULHSU 有符号*无符号高 32 位（-1*2 = -2，高位 = 0xFFFFFFFF）
  li t0, -1              # 有符号 -1
  li t1, 2               # 无符号 2
  mulhsu t2, t0, t1
  li t3, -1              # 0xFFFFFFFF
  bne t2, t3, fail6

  li a0, 0
  ebreak

fail1: li a0, 1; ebreak
fail2: li a0, 2; ebreak
fail3: li a0, 3; ebreak
fail4: li a0, 4; ebreak
fail5: li a0, 5; ebreak
fail6: li a0, 6; ebreak
