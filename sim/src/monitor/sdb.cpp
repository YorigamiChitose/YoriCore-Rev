#include "cpu/cpu.h"
#include "isa/isa.h"
#include "macro.h"
#include "monitor/monitor.h"
#include "memory/memory.h"
#include "utils.h"

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <readline/history.h>
#include <readline/readline.h>

static char *rl_gets(void) {
  static char *line_read = NULL;
  HIST_ENTRY *last_line = NULL;
  if (line_read) { free(line_read); line_read = NULL; }
  line_read = readline("(npc) ");
  if (line_read && *line_read) {
    add_history(line_read);
  } else if (line_read && history_length > 0) {
    last_line = history_get(history_length);
    free(line_read);
    line_read = (char *)malloc(strlen(last_line->line) + 1);
    memcpy(line_read, last_line->line, strlen(last_line->line) + 1);
  }
  return line_read;
}

static int cmd_help(char *args);
static int cmd_c(char *args);
static int cmd_q(char *args);
static int cmd_si(char *args);
static int cmd_sr(char *args);
static int cmd_info(char *args);
static int cmd_x(char *args);
static int cmd_p(char *args);

static struct {
  const char *name;
  const char *description;
  int (*handler)(char *);
} cmd_table[] = {
    {"help", "Usage: help\t[str]\t\t - Display information about commands.", cmd_help},
    {"c", "Usage: c\t\t\t - Continue the execution of the program.", cmd_c},
    {"q", "Usage: q\t\t\t - Exit NPC.", cmd_q},
    {"si", "Usage: si\t[int]\t\t - Run N retired instructions.", cmd_si},
    {"sr", "Usage: sr\t\t\t - Step one instruction and show registers.", cmd_sr},
    {"info", "Usage: info\tr | c\t\t - Get register/CSR info.", cmd_info},
    {"x", "Usage: x\t[N] [ADDR]\t - Scan N words of memory.", cmd_x},
    {"p", "Usage: p\t[EXPR]\t\t - Evaluate an expression.", cmd_p},
};

#define SDB_MAX_ARGS 16
#define NR_CMD ARRLEN(cmd_table)

static int split_args(char *args, char **argv, int max) {
  if (args == NULL) return 0;
  int argc = 0;
  char *p = args;
  while (argc < max) {
    while (*p == ' ') p++;
    if (*p == '\0') break;
    argv[argc++] = p;
    while (*p != '\0' && *p != ' ') p++;
    if (*p == ' ') { *p = '\0'; p++; }
  }
  return argc;
}

static int cmd_help(char *args) {
  char *argv[1];
  int argc = split_args(args, argv, 1);
  if (argc == 0) {
    for (int i = 0; i < NR_CMD; i++) printf("%-6s- %s\n", cmd_table[i].name, cmd_table[i].description);
  } else {
    for (int i = 0; i < NR_CMD; i++) {
      if (strcmp(argv[0], cmd_table[i].name) == 0) {
        printf("%-6s- %s\n", cmd_table[i].name, cmd_table[i].description);
        return 0;
      }
    }
    printf("Error! Unknown command '%s'\n", argv[0]);
  }
  return 0;
}

static int cmd_c(char *args) {
  (void)args;
  cpu_exec((uint64_t)-1);
  return 0;
}

static int cmd_q(char *args) {
  (void)args;
  if (npc_state.state == NPC_ABORT) npc_state.halt_ret = 1;
  if (npc_state.halt_ret == 0) npc_state.state = NPC_QUIT;
  return -1;
}

static int parse_count(char *args, uint64_t *n) {
  bool ok = true;
  if (args == NULL) { *n = 1; return 0; }
  word_t v = expr(args, &ok);
  if (!ok) { printf("Invalid count expression\n"); return -1; }
  *n = v;
  return 0;
}

static int cmd_si(char *args) {
  uint64_t n;
  if (parse_count(args, &n) < 0) return 0;
  cpu_exec(n);
  return 0;
}

static int cmd_sr(char *args) {
  (void)args;
  cpu_exec(1);
  if (npc_state.state != NPC_END) isa_reg_display();
  return 0;
}

static int cmd_info(char *args) {
  char *argv[1];
  int argc = split_args(args, argv, 1);
  if (argc == 0) { printf("Usage: info r | c\n"); return 0; }
  if (strcmp(argv[0], "r") == 0) isa_reg_display();
  else if (strcmp(argv[0], "c") == 0) isa_csr_display();
  else printf("Unknown info '%s'\n", argv[0]);
  return 0;
}

static int cmd_x(char *args) {
  char *argv[2];
  int argc = split_args(args, argv, 2);
  if (argc < 2) { printf("Usage: x N ADDR\n"); return 0; }
  bool ok = true;
  word_t n = expr(argv[0], &ok);
  word_t addr = ok ? expr(argv[1], &ok) : 0;
  if (!ok) { printf("Invalid expression\n"); return 0; }
  for (word_t i = 0; i < n; i++) {
    printf(FMT_WORD ": " FMT_WORD "\n", addr + i * 4, pmem_read(addr + i * 4, 4));
  }
  return 0;
}

static int cmd_p(char *args) {
  bool ok = true;
  word_t v = expr(args, &ok);
  if (ok) printf(FMT_WORD "\n", v);
  return 0;
}

static bool is_batch_mode = false;
void sdb_set_batch_mode(void) { is_batch_mode = true; }

void sdb_mainloop(void) {
  if (is_batch_mode) { cmd_c(NULL); return; }

  for (char *str; (str = rl_gets()) != NULL;) {
    char *str_end = str + strlen(str);
    char *cmd = strtok(str, " ");
    if (cmd == NULL) continue;
    char *args = cmd + strlen(cmd) + 1;
    if (args >= str_end) args = NULL;

    int i;
    for (i = 0; i < NR_CMD; i++) {
      if (strcmp(cmd, cmd_table[i].name) == 0) {
        if (cmd_table[i].handler(args) < 0) return;
        break;
      }
    }
    if (i == NR_CMD) printf("Unknown command '%s'\n", cmd);
  }
}

void init_sdb(void) { init_regex(); }
