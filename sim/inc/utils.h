#ifndef __UTILS_H__
#define __UTILS_H__

#include "common.h"

enum { NPC_RUNNING, NPC_STOP, NPC_END, NPC_ABORT, NPC_QUIT };

typedef struct {
  int state;
  vaddr_t halt_pc;
  uint32_t halt_ret;
} NPCState;

extern NPCState npc_state;

bool is_exit_status_bad(void);
void init_rand(void);
uint64_t get_time(void);

#define ANSI_FG_RED "\33[1;31m"
#define ANSI_FG_GREEN "\33[1;32m"
#define ANSI_FG_YELLOW "\33[1;33m"
#define ANSI_FG_BLUE "\33[1;34m"
#define ANSI_FG_MAGENTA "\33[1;35m"
#define ANSI_FG_CYAN "\33[1;36m"
#define ANSI_NONE "\33[0m"
#define ANSI_FMT(str, fmt) fmt str ANSI_NONE
#define _Log(...) do { printf(__VA_ARGS__); } while (0)

#endif
