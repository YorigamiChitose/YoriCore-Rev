#include "cpu/commit.h"

extern "C" void sim_commit(int pc, int npc, int inst, int priv, int exc,
                           int rd_en, int rd_addr, int rd_data,
                           int csr_en, int csr_addr, int csr_data,
                           int mem_valid, int mem_write, int mem_addr,
                           int mem_wdata, int mem_size) {
  CommitEvent e{};
  e.pc = (uint32_t)pc;
  e.npc = (uint32_t)npc;
  e.inst = (uint32_t)inst;
  e.priv = (uint8_t)priv;
  e.exc = (uint8_t)exc;
  e.rd_en = rd_en != 0;
  e.rd_addr = (uint8_t)rd_addr;
  e.rd_data = (uint32_t)rd_data;
  e.csr_en = csr_en != 0;
  e.csr_addr = (uint16_t)csr_addr;
  e.csr_data = (uint32_t)csr_data;
  e.mem_valid = mem_valid != 0;
  e.mem_write = mem_write != 0;
  e.mem_addr = (uint32_t)mem_addr;
  e.mem_wdata = (uint32_t)mem_wdata;
  e.mem_size = (uint8_t)mem_size;
  commit_enqueue(e);
}
