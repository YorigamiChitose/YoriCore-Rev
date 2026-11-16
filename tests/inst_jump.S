# RV32I 跳转链接指令测试
# 覆盖：JAL JALR（跳转 + 链接地址校验）
.section .text
.globl _start
_start:
  # JAL：跳转并链接，ra 应等于下一条指令地址
  jal ra, j_target
back:
  auipc t0, 0            # t0 = back 地址
  bne ra, t0, fail1      # ra 应 == back
  j j_done
j_target:
  j back
j_done:

  # JALR：寄存器跳转并链接
  la t0, jr_target
  jalr ra, t0, 0         # ra = jr_back
jr_back:
  auipc t1, 0            # t1 = jr_back 地址
  bne ra, t1, fail2      # ra 应 == jr_back
  j jr_done
jr_target:
  jr ra                  # 跳回 jr_back
jr_done:

  li a0, 0
  ebreak

fail1: li a0, 1; ebreak
fail2: li a0, 2; ebreak
