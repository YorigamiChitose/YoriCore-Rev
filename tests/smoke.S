# 最小启动冒烟测试：能取指、译码、执行 ebreak 并按 a0==0 判通过
.section .text
.globl _start
_start:
  li   a0, 0
  ebreak
