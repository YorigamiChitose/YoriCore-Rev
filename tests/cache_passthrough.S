# Cache 直通（MMIO）完备检测 (RV32IMA)
# 覆盖：可缓存读写、MMIO 字/字节/半字、非对齐地址、相邻地址无泄漏、MMIO 不污染缓存。
# 失败码 a0=1..8 精确定位出错项。
#
# Build:
#   riscv64-unknown-elf-gcc -march=rv32ima -mabi=ilp32 -nostdlib -nostartfiles \
#     -Ttext=0x80000000 -o cache_passthrough.elf cache_passthrough.S
#   riscv64-unknown-elf-objcopy -O binary cache_passthrough.elf cache_passthrough.bin
# Run:
#   build/verilator/VSimTop cache_passthrough.bin
.section .text
.globl _start
_start:
  li t0, 0x80000000        # 可缓存基址
  li t1, 0xF0000000        # MMIO 基址（不可缓存，直通）

  # T1: 可缓存 store/load（同一行）
  li t2, 0x12345678
  sw  t2, 0(t0)
  lw  t3, 0(t0)
  bne t3, t2, fail1

  # T2: 可缓存 store/load（不同行 0x80000020）
  li t4, 0x0BADF00D
  li t5, 32
  add t6, t0, t5
  sw  t4, 0(t6)
  lw  t3, 0(t6)
  bne t3, t4, fail2

  # T3: MMIO 字写（对齐 0xF0000010）读回
  li t2, 0xAABBCCDD
  sw  t2, 0x10(t1)
  lw  t3, 0x10(t1)
  bne t3, t2, fail3

  # T4: MMIO 字写（非 8 字节对齐 0xF0000004）读回
  li t2, 0x11223344
  sw  t2, 4(t1)
  lw  t3, 4(t1)
  bne t3, t2, fail4

  # T5: 相邻地址 0xF0000008 无泄漏（应为 0）
  lw  t3, 8(t1)
  bne t3, x0, fail5

  # T6: MMIO 字节写/读（奇地址 0xF0000015）
  li t2, 0xAB
  sb  t2, 0x15(t1)
  lbu t3, 0x15(t1)
  bne t3, t2, fail6

  # T7: MMIO 半字写/读（非字对齐 0xF000001A）
  li t2, 0xBEEF
  sh  t2, 0x1A(t1)
  lhu t3, 0x1A(t1)
  bne t3, t2, fail7

  # T8: MMIO 读未写入地址后，可缓存数据仍正确
  lw  t3, 0x20(t1)         # 未写入 MMIO -> 0
  li t2, 0x12345678
  lw  t3, 0(t0)            # 可缓存 [0x80000000] 仍为 0x12345678
  bne t3, t2, fail8

  li a0, 0
  ebreak

fail1: li a0, 1; ebreak
fail2: li a0, 2; ebreak
fail3: li a0, 3; ebreak
fail4: li a0, 4; ebreak
fail5: li a0, 5; ebreak
fail6: li a0, 6; ebreak
fail7: li a0, 7; ebreak
fail8: li a0, 8; ebreak
