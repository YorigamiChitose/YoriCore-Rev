# RV32I 分支指令测试
# 覆盖：BEQ BNE BLT BGE BLTU BGEU（各测 taken 与 not-taken）
.section .text
.globl _start
_start:
  # BEQ taken（相等）
  li t0, 5
  li t1, 5
  beq t0, t1, beq_ok
  j fail1
beq_ok:
  # BEQ not-taken（不等）
  li t0, 5
  li t1, 6
  beq t0, t1, fail2

  # BNE taken（不等）
  li t0, 5
  li t1, 6
  bne t0, t1, bne_ok
  j fail3
bne_ok:
  # BNE not-taken（相等）
  li t0, 5
  li t1, 5
  bne t0, t1, fail4

  # BLT taken（有符号 -2 < 3）
  li t0, -2
  li t1, 3
  blt t0, t1, blt_ok
  j fail5
blt_ok:
  # BLT not-taken（3 < -2 为假）
  li t0, 3
  li t1, -2
  blt t0, t1, fail6

  # BGE taken（相等也算 >=）
  li t0, 3
  li t1, 3
  bge t0, t1, bge_ok
  j fail7
bge_ok:
  # BGE not-taken（-2 >= 3 为假）
  li t0, -2
  li t1, 3
  bge t0, t1, fail8

  # BLTU taken（无符号 1 < 0xFFFFFFFF）
  li t0, 1
  li t1, -1
  bltu t0, t1, bltu_ok
  j fail9
bltu_ok:
  # BLTU not-taken（0xFFFFFFFF < 1 为假）
  li t0, -1
  li t1, 1
  bltu t0, t1, fail10

  # BGEU taken（无符号 0xFFFFFFFF >= 1）
  li t0, -1
  li t1, 1
  bgeu t0, t1, bgeu_ok
  j fail11
bgeu_ok:
  # BGEU not-taken（1 >= 0xFFFFFFFF 为假）
  li t0, 1
  li t1, -1
  bgeu t0, t1, fail12

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
