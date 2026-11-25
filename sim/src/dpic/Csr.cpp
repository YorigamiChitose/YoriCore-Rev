#include "isa/isa.h"

extern "C" void sim_csr_update(int mepc, int mcause, int mstatus, int mtvec, int priv,
                                int sepc, int scause, int stvec, int sie,
                                int sip, int sstatus, int satp) {
  cpu.csr.mepc = (uint32_t)mepc;
  cpu.csr.mcause = (uint32_t)mcause;
  cpu.csr.mstatus = (uint32_t)mstatus;
  cpu.csr.mtvec = (uint32_t)mtvec;
  cpu.csr.sepc = (uint32_t)sepc;
  cpu.csr.scause = (uint32_t)scause;
  cpu.csr.stvec = (uint32_t)stvec;
  cpu.csr.sie = (uint32_t)sie;
  cpu.csr.sip = (uint32_t)sip;
  cpu.csr.sstatus = (uint32_t)sstatus;
  cpu.csr.satp = (uint32_t)satp;
  cpu.mode = (uint8_t)priv;
}
