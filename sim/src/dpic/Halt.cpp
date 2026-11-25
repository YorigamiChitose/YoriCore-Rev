#include "cpu/cpu.h"
#include "isa/isa.h"
#include "utils.h"

#include <cstdio>

extern "C" void sim_halt(int code) {
  if (npc_state.state != NPC_RUNNING) return;
  if (code == 0) {
    printf(ANSI_FMT("HIT GOOD TRAP", ANSI_FG_GREEN) " at pc = " FMT_WORD "\n", cpu.pc);
    set_npc_state(NPC_END, cpu.pc, 0);
  } else {
    printf(ANSI_FMT("HIT BAD TRAP", ANSI_FG_RED) " (code=%d) at pc = " FMT_WORD "\n", code, cpu.pc);
    set_npc_state(NPC_END, cpu.pc, code);
  }
}
