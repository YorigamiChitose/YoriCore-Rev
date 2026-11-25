#include "isa/isa.h"
#include "difftest/difftest.h"

extern "C" void sim_csr_update(int mepc, int mcause, int mstatus, int mtvec, int priv) {
  static int last_mcause = -1;
  if (mcause != last_mcause && (mcause & 0x80000000)) difftest_skip_irq();
  last_mcause = mcause;
  cpu.csr.mepc = (uint32_t)mepc;
  cpu.csr.mcause = (uint32_t)mcause;
  cpu.csr.mstatus = (uint32_t)mstatus;
  cpu.csr.mtvec = (uint32_t)mtvec;
  cpu.mode = (uint8_t)priv;
  if (priv != 3) difftest_detach();
}
