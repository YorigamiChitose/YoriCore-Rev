#include "monitor/monitor.h"
#include "debug.h"
#include "cpu/commit.h"
#include "isa/isa.h"
#include "macro.h"
#include "memory/memory.h"
#include "verilator/verilator.h"

#include <cstdlib>
#include <getopt.h>

char *img_file = NULL;
void sdb_set_batch_mode(void);

static long load_img(void) {
  if (img_file == NULL) {
    fprintf(stderr, "No image is given. Usage: VSimTop [OPTION...] IMAGE\n");
    exit(1);
  }

  FILE *fp = fopen(img_file, "rb");
  Assert(fp, "Can not open '%s'", img_file);

  fseek(fp, 0, SEEK_END);
  long size = ftell(fp);
  Log("The image is %s, size = %ld", img_file, size);

  fseek(fp, 0, SEEK_SET);
  int ret = fread(guest_to_host(RESET_VECTOR), size, 1, fp);
  assert(ret == 1);
  fclose(fp);
  return size;
}

static int parse_args(int argc, char *argv[]) {
  const struct option table[] = {
      {"batch", no_argument, NULL, 'b'},
      {"help", no_argument, NULL, 'h'},
      {0, 0, NULL, 0},
  };
  int o;
  while ((o = getopt_long(argc, argv, "-bh", table, NULL)) != -1) {
    switch (o) {
    case 'b': sdb_set_batch_mode(); break;
    case 1: img_file = optarg; return 0;
    default:
      printf("Usage: %s [OPTION...] IMAGE\n\n", argv[0]);
      printf("\t-b,--batch              run with batch mode\n");
      printf("\n");
      exit(0);
    }
  }
  return 0;
}

static void welcome(void) {
#ifdef CONFIG_WAVE
  Log("Waveform: ON");
#else
  Log("Waveform: OFF");
#endif
  Log("Build time: %s, %s", __TIME__, __DATE__);
  printf("Welcome to %s-NPC!\n", ANSI_FMT("riscv32", ANSI_FG_YELLOW));
  printf("For help, type \"help\"\n");
}

void init_monitor(int argc, char *argv[]) {
  parse_args(argc, argv);
  init_rand();

  init_mem();
  init_isa();
  init_sdb();

  long img_size = load_img();
  init_verilator();
  perf_reset();

  welcome();
}
