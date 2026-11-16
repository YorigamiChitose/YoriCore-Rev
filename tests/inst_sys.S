# RV32I 系统指令测试
# 覆盖：FENCE FENCE_I ECALL（trap 跳转到 mtvec）
.section .text
.globl _start
_start:
  # FENCE / FENCE_I：作为 nop 执行后应继续
  fence
  fence.i
  li t0, 0xDEAD
  li t1, 0xDEAD
  bne t0, t1, fail1     # 状态未被破坏

  # ECALL：设置 mtvec 指向 handler 后触发 trap
  la t0, handler
  csrw mtvec, t0        # mtvec = handler 地址
  li a0, 0
  ecall                 # 触发 machine-mode ecall -> 跳转 handler
  j fail2               # 若未 trap（错误）

handler:
  # 校验 mcause == 11（machine ecall）
  csrr t0, mcause
  li t1, 11
  bne t0, t1, fail3
  # 校验 mepc 非零（指向 ecall 指令地址）
  csrr t0, mepc
  beq t0, x0, fail4
  # 通过
  li a0, 0
  ebreak

fail1: li a0, 1; ebreak
fail2: li a0, 2; ebreak
fail3: li a0, 3; ebreak
fail4: li a0, 4; ebreak
