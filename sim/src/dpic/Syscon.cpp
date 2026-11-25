#include "cpu/cpu.h"
#include "isa/isa.h"
#include "utils.h"

#include <cstdio>

extern "C" void sim_syscon(int reboot) {
  if (npc_state.state != NPC_RUNNING) return;
  if (reboot) {
    printf("SYSCON REBOOT\n");
    set_npc_state(NPC_QUIT, cpu.pc, 0);
  } else {
    printf("SYSCON EXIT\n");
    set_npc_state(NPC_END, cpu.pc, 0);
  }
}
