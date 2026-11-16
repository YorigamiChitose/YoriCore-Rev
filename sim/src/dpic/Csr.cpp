#include "isa/isa.h"

extern "C" void sim_csr_update(int mepc, int mcause, int mstatus, int mtvec, int priv) {
  cpu.csr.mepc = (uint32_t)mepc;
  cpu.csr.mcause = (uint32_t)mcause;
  cpu.csr.mstatus = (uint32_t)mstatus;
  cpu.csr.mtvec = (uint32_t)mtvec;
  cpu.mode = (uint8_t)priv;
}
