#include "cpu/cpu.h"
#include "cpu/commit.h"
#include "debug.h"
#include "difftest/difftest.h"
#include "isa/isa.h"
#include "macro.h"
#include "memory/memory.h"
#include "utils.h"
#include "verilator/verilator.h"

#include <cstdio>
#include <cstdint>
#include <deque>

static std::deque<CommitEvent> commit_queue;

void commit_enqueue(const CommitEvent &e) { commit_queue.push_back(e); }
bool commit_dequeue(CommitEvent &e) {
  if (commit_queue.empty()) return false;
  e = commit_queue.front();
  commit_queue.pop_front();
  return true;
}

uint64_t g_nr_guest_inst = 0;
uint64_t g_timer = 0;
static uint64_t g_steps = 0;
static uint64_t g_step_limit = CONFIG_MAX_CYCLES;

static void apply_commit(const CommitEvent &c) {
  cpu.pc = c.npc;
  if (c.rd_en && c.rd_addr != 0) cpu.gpr[c.rd_addr] = c.rd_data;
  cpu.gpr[0] = 0;
}

// HALT 由 RTL 设备完成；提交级只负责跳过参考模型对比。
static bool handle_halt(const CommitEvent &c) {
  if (!(c.mem_valid && c.mem_addr == (uint32_t)CONFIG_HALT_ADDR)) return false;
  difftest_skip_ref();
  return true;
}

static bool is_rtl_mmio(uint32_t addr) {
  return (addr >= (uint32_t)CONFIG_SYSCON_ADDR && addr < (uint32_t)CONFIG_SYSCON_ADDR + 0x1000u) ||
         (addr >= (uint32_t)CONFIG_CLINT_ADDR && addr < (uint32_t)CONFIG_CLINT_ADDR + 0xc000u) ||
         (addr >= (uint32_t)CONFIG_PLIC_ADDR && addr < (uint32_t)CONFIG_PLIC_ADDR + 0x400000u) ||
         (addr >= (uint32_t)CONFIG_RTC_ADDR && addr < (uint32_t)CONFIG_RTC_ADDR + 0x1000u) ||
         (addr >= (uint32_t)CONFIG_SERIAL_PORT && addr < (uint32_t)CONFIG_SERIAL_PORT + 0x1000u);
}

static bool difftest_skip_inst(uint32_t inst) {
  return inst == 0x10500073u || // wfi
         inst == 0x0ff0000fu || // fence
         inst == 0x00100073u || // ebreak
         inst == 0x00009002u;   // c.ebreak
}

static bool drain_commits(void) {
  bool retired = false;
  CommitEvent c;
  while (commit_dequeue(c)) {
    retired = true;
    apply_commit(c);
    g_nr_guest_inst++;

    bool handled = handle_halt(c);
    if (npc_state.state != NPC_RUNNING) return true;
    if (handled) continue;

    if ((cpu.csr.mcause & 0x80000000u) && c.pc == cpu.csr.mtvec) {
      difftest_skip_ref();
    } else if (c.mem_valid && is_rtl_mmio(c.mem_addr)) {
      difftest_skip_ref();
    } else if (difftest_skip_inst(c.inst)) {
      difftest_skip_ref();
    }
    difftest_step(c.pc, c.npc);
    if (npc_state.state != NPC_RUNNING) return true;
  }
  return retired;
}

// 一次 exec_once = 一条正常提交（退休）指令，而不是一个时钟。
static void exec_once(void) {
  while (npc_state.state == NPC_RUNNING) {
    step_verilator();
    g_steps++;

    if (drain_commits()) return;
    if (g_steps > g_step_limit) {
      printf(ANSI_FMT("TIMEOUT", ANSI_FG_RED) " after %llu cycles without retirement\n",
             (unsigned long long)g_steps);
      set_npc_state(NPC_ABORT, cpu.pc, -1);
      return;
    }
  }
}

static double perf_ratio(uint64_t a, uint64_t b) {
  return b == 0 ? 0.0 : (double)a / (double)b;
}

void statistic(void) {
  Log("host time spent = %llu us", (unsigned long long)g_timer);
  Log("total guest instructions = %llu", (unsigned long long)g_nr_guest_inst);
  if (g_timer > 0)
    Log("simulation frequency = %llu inst/s", (unsigned long long)(g_nr_guest_inst * 1000000 / g_timer));
  else
    Log("finish running in less than 1 us");

  Log("perf: cycles = %llu, instret = %llu, IPC = %.3f",
      (unsigned long long)g_perf.cycles,
      (unsigned long long)g_perf.instret,
      perf_ratio(g_perf.instret, g_perf.cycles));
  Log("perf: I$ hit = %llu, miss = %llu, hit rate = %.3f",
      (unsigned long long)g_perf.i_hit,
      (unsigned long long)g_perf.i_miss,
      perf_ratio(g_perf.i_hit, g_perf.i_hit + g_perf.i_miss));
  Log("perf: D$ hit = %llu, miss = %llu, hit rate = %.3f",
      (unsigned long long)g_perf.d_hit,
      (unsigned long long)g_perf.d_miss,
      perf_ratio(g_perf.d_hit, g_perf.d_hit + g_perf.d_miss));
  Log("perf: fence = %llu, fence.i = %llu, wfi = %llu",
      (unsigned long long)g_perf.fence,
      (unsigned long long)g_perf.fence_i,
      (unsigned long long)g_perf.wfi);
}

void assert_fail_msg(void) { statistic(); }

void cpu_exec(uint64_t n) {
  switch (npc_state.state) {
  case NPC_END:
  case NPC_ABORT:
  case NPC_QUIT:
    printf("Program execution has ended. To restart the program, exit and run again.\n");
    return;
  default:
    npc_state.state = NPC_RUNNING;
  }

  g_steps = 0;
  g_step_limit = CONFIG_MAX_CYCLES;
  uint64_t timer_start = get_time();

  if (n == (uint64_t)-1) {
    while (npc_state.state == NPC_RUNNING) exec_once();
  } else {
    for (uint64_t i = 0; i < n && npc_state.state == NPC_RUNNING; i++) exec_once();
  }

  uint64_t timer_end = get_time();
  g_timer += timer_end - timer_start;

  switch (npc_state.state) {
  case NPC_RUNNING:
    npc_state.state = NPC_STOP;
    break;
  case NPC_END:
  case NPC_ABORT:
    Log("npc: %s at pc = " FMT_WORD,
        (npc_state.state == NPC_ABORT
             ? ANSI_FMT("ABORT", ANSI_FG_RED)
             : (npc_state.halt_ret == 0 ? ANSI_FMT("HIT GOOD TRAP", ANSI_FG_GREEN)
                                        : ANSI_FMT("HIT BAD TRAP", ANSI_FG_RED))),
        npc_state.halt_pc);
    statistic();
    break;
  case NPC_QUIT:
    statistic();
    break;
  default:
    break;
  }
}
