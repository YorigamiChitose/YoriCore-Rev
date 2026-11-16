#ifndef __VERILATOR_H__
#define __VERILATOR_H__

#include <VSimTop.h>
#include <verilated.h>
#include <verilated_vcd_c.h>

#include "common.h"

extern VSimTop *vtop;

inline void eval_verilator(void) {
  vtop->eval();
#ifdef CONFIG_WAVE
  extern VerilatedVcdC *tfp;
  extern VerilatedContext *contextp;
  contextp->timeInc(1);
  tfp->dump(contextp->time());
#endif
}

void init_verilator(void);
void step_verilator(void);
void exit_verilator(void);

#endif
