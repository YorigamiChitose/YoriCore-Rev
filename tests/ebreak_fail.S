# 负向测试：ebreak 时 a0!=0，应判失败（预期退出码 1）
.section .text
.globl _start
_start:
  li   a0, 1
  ebreak
