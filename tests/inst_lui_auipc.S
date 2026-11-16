# RV32I LUI / AUIPC 指令测试
.section .text
.globl _start
_start:
  # LUI：装载高 20 位立即数，低 12 位清零
  lui t0, 0x12345          # t0 = 0x12345000
  li t1, 0x12345000
  bne t0, t1, fail1

  # LUI 负数高 20 位（bit19=1）
  lui t0, 0x80000          # t0 = 0x80000000
  li t1, 0x80000000
  bne t0, t1, fail2

  # AUIPC：PC + (imm << 12)，结果高 20 位近似 PC
  auipc t0, 0              # t0 = 当前 PC（本指令地址）
  # 校验：t0 非零且落在代码段（0x80000000 附近）
  beq t0, x0, fail3
  # t0 应为 4 字节对齐
  andi t1, t0, 3
  bne t1, x0, fail4

  # AUIPC + 偏移：配合 addi 得到下一条指令地址（标准 la 模式）
  auipc t0, 0
  addi t1, t0, 0           # t1 = t0
  bne t0, t1, fail5

  li a0, 0
  ebreak

fail1: li a0, 1; ebreak
fail2: li a0, 2; ebreak
fail3: li a0, 3; ebreak
fail4: li a0, 4; ebreak
fail5: li a0, 5; ebreak
