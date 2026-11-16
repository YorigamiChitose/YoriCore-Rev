# Cache write-back (eviction) directed test (RV32IMA)
# Verifies: dirty-line eviction writes data back to memory, then re-fill reads it back.
# All 5 lines below map to the same cache set (index = bits[10:5] = 0), distinct tags.
# 4 dirty lines fill all ways; the 5th access forces an eviction -> write-back to memory.
# Re-reading the evicted line afterwards re-fills from memory, proving write-back persisted it.
#
# Build:
#   riscv64-unknown-elf-gcc -march=rv32ima -mabi=ilp32 -nostdlib -nostartfiles \
#     -Ttext=0x80000000 -o cache_writeback.elf cache_writeback.S
#   riscv64-unknown-elf-objcopy -O binary cache_writeback.elf cache_writeback.bin
# Run:
#   build/verilator/VSimTop cache_writeback.bin
.section .text
.globl _start
_start:
  # ---- fill 4 dirty lines in set 0 ----
  li t0, 0x80000000
  li t1, 0x11111111
  sw  t1, 0(t0)

  li t0, 0x80000800
  li t1, 0x22222222
  sw  t1, 0(t0)

  li t0, 0x80001000
  li t1, 0x33333333
  sw  t1, 0(t0)

  li t0, 0x80001800
  li t1, 0x44444444
  sw  t1, 0(t0)

  # ---- read back (hit check) ----
  li t0, 0x80000000
  lw  t2, 0(t0)
  li  t3, 0x11111111
  bne t2, t3, fail

  li t0, 0x80000800
  lw  t2, 0(t0)
  li  t3, 0x22222222
  bne t2, t3, fail

  li t0, 0x80001000
  lw  t2, 0(t0)
  li  t3, 0x33333333
  bne t2, t3, fail

  li t0, 0x80001800
  lw  t2, 0(t0)
  li  t3, 0x44444444
  bne t2, t3, fail

  # ---- 5th line forces eviction of a dirty way (write-back) ----
  li t0, 0x80002000
  li t1, 0x55555555
  sw  t1, 0(t0)
  lw  t2, 0(t0)
  li  t3, 0x55555555
  bne t2, t3, fail

  # ---- re-read 4 lines: evicted one must re-fill from memory (write-back proof) ----
  li t0, 0x80000000
  lw  t2, 0(t0)
  li  t3, 0x11111111
  bne t2, t3, fail

  li t0, 0x80000800
  lw  t2, 0(t0)
  li  t3, 0x22222222
  bne t2, t3, fail

  li t0, 0x80001000
  lw  t2, 0(t0)
  li  t3, 0x33333333
  bne t2, t3, fail

  li t0, 0x80001800
  lw  t2, 0(t0)
  li  t3, 0x44444444
  bne t2, t3, fail

  li  a0, 0
  ebreak

fail:
  li  a0, 1
  ebreak
