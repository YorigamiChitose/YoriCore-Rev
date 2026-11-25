#ifndef __ISA_H__
#define __ISA_H__

#include "common.h"

extern const char *regs[];

typedef struct {
  vaddr_t mepc;
  word_t mcause;
  word_t mstatus;
  word_t mtvec;
  word_t sepc;
  word_t scause;
  word_t stvec;
  word_t sie;
  word_t sip;
  word_t sstatus;
  word_t satp;
} CSR;

typedef struct {
  word_t gpr[32];
  vaddr_t pc;
  CSR csr;
  uint8_t mode;
} CPU_state;

extern CPU_state cpu;

void init_isa(void);
void engine_start(void);
void isa_reg_display(void);
void isa_csr_display(void);
word_t isa_reg_str2val(const char *s, bool *success);
void set_npc_state(int state, vaddr_t pc, int halt_ret);

#endif
