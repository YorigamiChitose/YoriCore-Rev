#include "cpu/commit.h"

#include <deque>

PerfState g_perf = {};
void perf_reset(void) { g_perf = {}; }

static uint64_t join_u64(int lo, int hi) {
  return (uint64_t)(uint32_t)hi << 32 | (uint32_t)lo;
}

extern "C" void sim_perf_update(
    int cycles_lo, int cycles_hi,
    int instret_lo, int instret_hi,
    int i_hit_lo, int i_hit_hi,
    int i_miss_lo, int i_miss_hi,
    int d_hit_lo, int d_hit_hi,
    int d_miss_lo, int d_miss_hi,
    int fence_lo, int fence_hi,
    int fence_i_lo, int fence_i_hi,
    int wfi_lo, int wfi_hi) {
  g_perf.cycles = join_u64(cycles_lo, cycles_hi);
  g_perf.instret = join_u64(instret_lo, instret_hi);
  g_perf.i_hit = join_u64(i_hit_lo, i_hit_hi);
  g_perf.i_miss = join_u64(i_miss_lo, i_miss_hi);
  g_perf.d_hit = join_u64(d_hit_lo, d_hit_hi);
  g_perf.d_miss = join_u64(d_miss_lo, d_miss_hi);
  g_perf.fence = join_u64(fence_lo, fence_hi);
  g_perf.fence_i = join_u64(fence_i_lo, fence_i_hi);
  g_perf.wfi = join_u64(wfi_lo, wfi_hi);
}

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
