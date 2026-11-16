# jal 跳转测试：jal 跳过失败路径，落在通过路径
.section .text
.globl _start
_start:
  j     pass        # jal x0, pass（跳过失败路径）
  li    a0, 1       # 失败路径（应被跳过）
  ebreak
pass:
  li    a0, 0
  ebreak
