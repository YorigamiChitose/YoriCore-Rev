# store 测试：存入内存后读回校验
.section .text
.globl _start
_start:
  li    t0, 0x80000000
  li    t1, 0xDEADBEEF
  sw    t1, 0(t0)     # 存
  lw    t2, 0(t0)     # 取
  bne   t1, t2, fail
  li    a0, 0
  ebreak
fail:
  li    a0, 1
  ebreak
