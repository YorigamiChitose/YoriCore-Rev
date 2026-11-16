#include "verilator/verilator.h"
#include "isa/isa.h"

CPU_state cpu = {};
VSimTop *vtop = NULL;
#ifdef CONFIG_WAVE
VerilatedContext *contextp = NULL;
VerilatedVcdC *tfp = NULL;
#endif

void init_verilator(void) {
#ifdef CONFIG_WAVE
  contextp = new VerilatedContext;
  tfp = new VerilatedVcdC;
#endif
  vtop = new VSimTop;
#ifdef CONFIG_WAVE
  contextp->traceEverOn(true);
  vtop->trace(tfp, 3);
  tfp->open("wave.vcd");
#endif
  vtop->reset = 1;
  vtop->clock = 0;
  eval_verilator();
  vtop->clock = 1;
  eval_verilator();
  vtop->reset = 0;
  vtop->clock = 0;
  eval_verilator();
}

void step_verilator(void) {
  vtop->clock = 1;
  eval_verilator();
  vtop->clock = 0;
  eval_verilator();
}

void exit_verilator(void) {
#ifdef CONFIG_WAVE
  if (tfp) {
    tfp->close();
    delete tfp;
    tfp = NULL;
  }
  if (contextp) {
    delete contextp;
    contextp = NULL;
  }
#endif
  if (vtop) {
    vtop->final();
    delete vtop;
    vtop = NULL;
  }
}
