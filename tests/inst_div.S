# RV32M 除法指令测试
# 覆盖：DIV DIVU REM REMU（含除零、溢出边界）
.section .text
.globl _start
_start:
  # DIV
  li t0, 10
  li t1, 3
  div t2, t0, t1         # 3
  li t3, 3
  bne t2, t3, fail1

  # DIV 负数（向零截断）
  li t0, -10
  li t1, 3
  div t2, t0, t1         # -3
  li t3, -3
  bne t2, t3, fail2

  # DIV 除零 -> 0xFFFFFFFF
  li t0, 10
  li t1, 0
  div t2, t0, t1
  li t3, -1
  bne t2, t3, fail3

  # DIV 溢出（0x80000000 / -1 -> 0x80000000）
  li t0, 0x80000000
  li t1, -1
  div t2, t0, t1
  li t3, 0x80000000
  bne t2, t3, fail4

  # DIVU
  li t0, 10
  li t1, 3
  divu t2, t0, t1        # 3
  li t3, 3
  bne t2, t3, fail5

  # DIVU 除零 -> 0xFFFFFFFF
  li t0, 10
  li t1, 0
  divu t2, t0, t1
  li t3, -1
  bne t2, t3, fail6

  # REM
  li t0, 10
  li t1, 3
  rem t2, t0, t1         # 1
  li t3, 1
  bne t2, t3, fail7

  # REM 负数（符号跟随被除数）
  li t0, -10
  li t1, 3
  rem t2, t0, t1         # -1
  li t3, -1
  bne t2, t3, fail8

  # REM 除零 -> 被除数
  li t0, 10
  li t1, 0
  rem t2, t0, t1         # 10
  li t3, 10
  bne t2, t3, fail9

  # REMU
  li t0, 10
  li t1, 3
  remu t2, t0, t1        # 1
  li t3, 1
  bne t2, t3, fail10

  # REMU 除零 -> 被除数
  li t0, 0x80000000
  li t1, 0
  remu t2, t0, t1        # 0x80000000
  li t3, 0x80000000
  bne t2, t3, fail11

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
