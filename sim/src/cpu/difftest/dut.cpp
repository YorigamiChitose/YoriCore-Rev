#include "difftest/difftest.h"
#include "cpu/commit.h"

#include <dlfcn.h>

#include "cpu/cpu.h"
#include "debug.h"
#include "isa/isa.h"
#include "memory/memory.h"
#include "utils.h"

void (*ref_difftest_memcpy)(paddr_t addr, void *buf, size_t n, bool direction) = NULL;
void (*ref_difftest_regcpy)(void *dut, bool direction) = NULL;
void (*ref_difftest_exec)(uint64_t n) = NULL;
void (*ref_difftest_raise_intr)(uint64_t NO) = NULL;

bool enable_difftest = false;
static bool is_skip_ref = false;
static int skip_dut_nr_inst = 0;

void difftest_skip_ref(void) {
  if (!enable_difftest) return;
  is_skip_ref = true;
  skip_dut_nr_inst = 0;
}

void difftest_skip_dut(int nr_ref, int nr_dut) {
  if (!enable_difftest) return;
  skip_dut_nr_inst += nr_dut;
  while (nr_ref-- > 0) {
    ref_difftest_exec(1);
  }
}

void difftest_detach(void) {
  enable_difftest = false;
}

void difftest_attach(void) {
  if (ref_difftest_regcpy == NULL) return;
  ref_difftest_regcpy(&cpu, DIFFTEST_TO_REF);
  enable_difftest = true;
}

static void checkregs(CPU_state *ref, vaddr_t pc) {
  if (!isa_difftest_checkregs(ref, pc)) {
    npc_state.state = NPC_ABORT;
    npc_state.halt_pc = pc;
    isa_reg_display();
  }
}

void init_difftest(char *ref_so_file, long img_size, int port) {
  if (ref_so_file == NULL) {
    Log("Difftest: OFF");
    return;
  }

  void *handle = dlopen(ref_so_file, RTLD_LAZY | RTLD_LOCAL);
  Assert(handle != NULL, "dlopen %s failed: %s", ref_so_file, dlerror());

  ref_difftest_memcpy = (decltype(ref_difftest_memcpy))dlsym(handle, "difftest_memcpy");
  ref_difftest_regcpy = (decltype(ref_difftest_regcpy))dlsym(handle, "difftest_regcpy");
  ref_difftest_exec   = (decltype(ref_difftest_exec))dlsym(handle, "difftest_exec");
  ref_difftest_raise_intr = (decltype(ref_difftest_raise_intr))dlsym(handle, "difftest_raise_intr");
  auto ref_init = (void (*)(int))dlsym(handle, "difftest_init");

  Assert(ref_difftest_memcpy && ref_difftest_regcpy && ref_difftest_exec && ref_init,
         "dlsym failed in %s", ref_so_file);

  ref_init(port);
  ref_difftest_memcpy(RESET_VECTOR, guest_to_host(RESET_VECTOR), img_size, DIFFTEST_TO_REF);
  ref_difftest_regcpy(&cpu, DIFFTEST_TO_REF);
  enable_difftest = true;
  Log("Difftest: ON (%s)", ref_so_file);
}

void difftest_step(vaddr_t pc, vaddr_t npc) {
  if (!enable_difftest) return;

  CPU_state ref_r;

  if (skip_dut_nr_inst > 0) {
    ref_difftest_regcpy(&ref_r, DIFFTEST_TO_DUT);
    if (ref_r.pc == npc) {
      skip_dut_nr_inst = 0;
      checkregs(&ref_r, npc);
      return;
    }
    skip_dut_nr_inst--;
    if (skip_dut_nr_inst == 0) {
      Log("can not catch up with ref.pc = " FMT_WORD " at pc = " FMT_WORD, ref_r.pc, pc);
      npc_state.state = NPC_ABORT;
      npc_state.halt_pc = pc;
    }
    return;
  }

  if (is_skip_ref) {
    ref_difftest_regcpy(&cpu, DIFFTEST_TO_REF);
    is_skip_ref = false;
    return;
  }

  ref_difftest_exec(1);
  ref_difftest_regcpy(&ref_r, DIFFTEST_TO_DUT);
  checkregs(&ref_r, pc);
  (void)npc;
}
