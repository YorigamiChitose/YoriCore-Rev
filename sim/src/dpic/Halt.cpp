#include "cpu/cpu.h"
#include "isa/isa.h"
#include "utils.h"

#include <cstdio>
#include <cstdlib>

extern "C" void sim_halt(int code) {
  if (npc_state.state != NPC_RUNNING) return;
  if (code == 0) {
    printf(ANSI_FMT("HIT GOOD TRAP", ANSI_FG_GREEN) " at pc = " FMT_WORD "\n", cpu.pc);
  } else {
    printf(ANSI_FMT("HIT BAD TRAP", ANSI_FG_RED) " (code=%d) at pc = " FMT_WORD "\n", code, cpu.pc);
  }
  fflush(stdout);
  exit(code == 0 ? 0 : code);
}
