#ifndef __DIFFTEST_H__
#define __DIFFTEST_H__

#include "common.h"
#include <cstddef>
#include "isa/isa.h"

#define __EXPORT __attribute__((visibility("default")))
enum { DIFFTEST_TO_DUT, DIFFTEST_TO_REF };

#define RISCV_GPR_NUM 32
#define DIFFTEST_REG_SIZE (sizeof(uint32_t) * (RISCV_GPR_NUM + 1))

extern void (*ref_difftest_memcpy)(paddr_t addr, void *buf, size_t n, bool direction);
extern void (*ref_difftest_regcpy)(void *dut, bool direction);
extern void (*ref_difftest_exec)(uint64_t n);
extern void (*ref_difftest_raise_intr)(uint64_t NO);

void init_difftest(char *ref_so_file, long img_size, int port);
void difftest_step(vaddr_t pc, vaddr_t npc);
void difftest_skip_ref(void);
void difftest_skip_dut(int nr_ref, int nr_dut);
void difftest_skip_irq(void);
void difftest_detach(void);
void difftest_attach(void);

#endif
