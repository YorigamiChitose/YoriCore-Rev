# 非对齐 MMIO 写定向测试：验证 AXI 窄传输 size/对齐 bug
# 写一个字到 0xF0000004（非 8 字节对齐），检查数据是否泄漏到相邻地址 0xF0000008
.section .text
.globl _start
_start:
  li   t0, 0xF0000004      # 非 8 字节对齐的 MMIO 地址（byteOff=4）
  li   t1, 0x11223344
  sw   t1, 0(t0)           # 字写 0xF0000004

  # 读回 0xF0000004 应得到 0x11223344（自洽）
  lw   t2, 0(t0)
  bne  t2, t1, fail1

  # 0xF0000008 应保持 0（数据不应泄漏到此处）
  lw   t2, 4(t0)           # 读 0xF0000008
  bne  t2, x0, fail2       # bug：此处会是 0x11223344

  li   a0, 0
  ebreak

fail1:
  li   a0, 1
  ebreak

fail2:
  li   a0, 2
  ebreak
