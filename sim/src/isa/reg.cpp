#include "cpu/cpu.h"
#include "debug.h"
#include "isa/isa.h"
#include "utils.h"
#include <cstdio>
#include <cstring>

const char *regs[] = {"$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
                      "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
                      "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
                      "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"};

void isa_reg_display(void) {
  cpu.gpr[0] = 0;
  for (int i = 0; i < 8; i++) {
    for (int j = 0; j < 4; j++) {
      printf(ANSI_FG_GREEN "%-3s" ANSI_NONE ": " FMT_WORD "\t", reg_name(i * 4 + j), gpr(i * 4 + j));
    }
    printf("\n");
  }
  printf(ANSI_FG_RED "%-7s" ANSI_NONE ": " FMT_WORD "\n", "pc", cpu.pc);
  printf(ANSI_FG_RED "%-7s" ANSI_NONE ": %d\n", "mode", cpu.mode);
}

void isa_csr_display(void) {
  printf(ANSI_FG_RED "%-7s" ANSI_NONE ": " FMT_WORD "\n", "mepc", cpu.csr.mepc);
  printf(ANSI_FG_RED "%-7s" ANSI_NONE ": " FMT_WORD "\n", "mcause", cpu.csr.mcause);
  printf(ANSI_FG_RED "%-7s" ANSI_NONE ": " FMT_WORD "\n", "mstatus", cpu.csr.mstatus);
  printf(ANSI_FG_RED "%-7s" ANSI_NONE ": " FMT_WORD "\n", "mtvec", cpu.csr.mtvec);
}

word_t isa_reg_str2val(const char *s, bool *success) {
  if (strcmp(s, "pc") == 0) { *success = true; return cpu.pc; }
  if (strcmp(s, "mepc") == 0) { *success = true; return cpu.csr.mepc; }
  if (strcmp(s, "mcause") == 0) { *success = true; return cpu.csr.mcause; }
  if (strcmp(s, "mstatus") == 0) { *success = true; return cpu.csr.mstatus; }
  if (strcmp(s, "mtvec") == 0) { *success = true; return cpu.csr.mtvec; }
  for (int i = 0; i < 32; i++) {
    if (strcmp(s, regs[i]) == 0) {
      *success = true;
      return gpr(i);
    }
  }
  *success = false;
  printf("Reg name error, Please retry!\n");
  return 0;
}
