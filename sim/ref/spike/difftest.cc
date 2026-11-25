// Spike 参考模型的 difftest_* 导出层（C++）。
// 参考 ysyx-workbench/nemu/tools/spike-diff，但只使用本地 Spike 的公共 API。
#include <sys/syscall.h>
#include <unistd.h>

#include <autoconf/autoconf.h>
#include <cassert>
#include <cstdint>
#include <cstring>
#include <optional>
#include <vector>

#include "cfg.h"
#include "devices.h"
#include "mmu.h"
#include "processor.h"
#include "sim.h"
#include "trap.h"

#define __EXPORT __attribute__((visibility("default")))
enum { DIFFTEST_TO_DUT, DIFFTEST_TO_REF };

struct diff_context_t {
  uint32_t gpr[32];
  uint32_t pc;
  uint32_t mepc;
  uint32_t mcause;
  uint32_t mstatus;
  uint32_t mtvec;
  uint8_t mode;
};

static sim_t *s = nullptr;
static processor_t *p = nullptr;
static state_t *state = nullptr;
static std::vector<std::pair<reg_t, abstract_mem_t *>> difftest_mem;

static void diff_get_regs(void *ctx_ptr) {
  auto *ctx = (diff_context_t *)ctx_ptr;
  for (int i = 0; i < 32; i++) ctx->gpr[i] = (uint32_t)state->XPR[i];
  ctx->pc = (uint32_t)state->pc;
  ctx->mepc = (uint32_t)state->mepc->read();
  ctx->mcause = (uint32_t)state->mcause->read();
  ctx->mstatus = (uint32_t)state->mstatus->read();
  ctx->mtvec = (uint32_t)state->mtvec->read();
  ctx->mode = (uint8_t)state->prv;
}

static void diff_set_regs(void *ctx_ptr) {
  auto *ctx = (diff_context_t *)ctx_ptr;
  for (int i = 0; i < 32; i++) state->XPR.write(i, ctx->gpr[i]);
  state->pc = ctx->pc;
  state->mepc->write(ctx->mepc);
  state->mcause->write(ctx->mcause);
  state->mstatus->write(ctx->mstatus);
  state->mtvec->write(ctx->mtvec);
  state->prv = ctx->mode;
}

static void diff_memcpy(uint32_t dst, const void *src, size_t n) {
  auto *mmu = p->get_mmu();
  auto *bytes = (const uint8_t *)src;
  for (size_t i = 0; i < n; i++) mmu->store<uint8_t>(dst + i, bytes[i]);
}

extern "C" {

__EXPORT void difftest_memcpy(uint32_t addr, void *buf, size_t n, bool direction) {
  if (direction == DIFFTEST_TO_REF) {
    diff_memcpy(addr, buf, n);
  } else {
    assert(0 && "TO_DUT memcpy is not supported by Spike ref");
  }
}

__EXPORT void difftest_regcpy(void *dut, bool direction) {
  if (direction == DIFFTEST_TO_REF) diff_set_regs(dut);
  else diff_get_regs(dut);
}

__EXPORT void difftest_exec(uint64_t n) {
  p->step((size_t)n);
}

__EXPORT void difftest_raise_intr(uint64_t NO) {
  (void)NO;
  assert(0 && "difftest_raise_intr is not supported yet");
}

__EXPORT void difftest_init(int port) {
  (void)port;

  static cfg_t cfg;
  cfg.isa = "RV32IMAC";
  cfg.priv = "MSU";
  cfg.mem_layout = {mem_cfg_t(CONFIG_MBASE, CONFIG_MSIZE)};
  cfg.hartids = {0};
  cfg.explicit_hartids = true;
  cfg.trigger_count = 4;
  cfg.start_pc.set_global(CONFIG_MBASE + CONFIG_PC_RESET_OFFSET);

  difftest_mem.clear();
  difftest_mem.emplace_back((reg_t)CONFIG_MBASE, new mem_t((reg_t)CONFIG_MSIZE));

  static debug_module_config_t dm_config;
  std::vector<std::string> htif_args = {""};
  s = new sim_t(&cfg, false, difftest_mem, {}, false, htif_args, dm_config,
                nullptr, false, nullptr, false, nullptr, std::nullopt);
  p = s->get_core(0);
  state = p->get_state();
}

} // extern "C"
