#include <VSimTop.h>
#include <verilated.h>
#include <verilated_vcd_c.h>
#include <verilated_sym_props.h>

#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>

#include "simconfig.h"

static VSimTop  *dut      = NULL;
static uint8_t   pmem[CONFIG_MSIZE];
static int       g_tohost = 0;
static int       g_halted = 0;
static uint64_t  cycle    = 0;

// RegFile / CSRFile 镜像指针（由 set_gpr_ptr / set_csr_ptr 注入）
static uint32_t *g_gpr = NULL;
static uint32_t *g_csr = NULL;

#ifdef CONFIG_WAVE
static VerilatedVcdC *tfp      = NULL;
static vluint64_t     simtime  = 0;
#endif

// —— DPI-C 实现（被 SimAXIRam.sv 调用）——
// 读 32 位字（低字节在低 8 位）
extern "C" int dpic_mem_read(int addr) {
  uint32_t a = (uint32_t)addr;
  if (a >= CONFIG_MBASE && a + 4 <= CONFIG_MBASE + CONFIG_MSIZE) {
    uint32_t off = a - CONFIG_MBASE;
    return (int)((uint32_t)pmem[off] |
                 ((uint32_t)pmem[off + 1] << 8) |
                 ((uint32_t)pmem[off + 2] << 16) |
                 ((uint32_t)pmem[off + 3] << 24));
  }
  return 0; // MMIO 读返回 0
}

// 写 32 位字（strb 为 4 位字节使能；低 bit 对应低字节）
extern "C" void dpic_mem_write(int addr, int data, int strb) {
  uint32_t a = (uint32_t)addr;
  if (a == CONFIG_TOHOST) {
    g_tohost = data;
    g_halted = 1;
    return;
  }
  if (a >= CONFIG_MBASE && a + 4 <= CONFIG_MBASE + CONFIG_MSIZE) {
    uint32_t off = a - CONFIG_MBASE;
    for (int i = 0; i < 4; i++) {
      if (strb & (1u << i)) pmem[off + i] = (uint8_t)(((uint32_t)data >> (8 * i)) & 0xFF);
    }
  }
}

// —— 寄存器 / CSR 镜像指针注入（被 RegFileTrace.sv / CsrFileTrace.sv 调用）——
extern "C" void set_gpr_ptr(const svOpenArrayHandle r) {
  VerilatedDpiOpenVar *g = (VerilatedDpiOpenVar *)r;
  g_gpr                  = (uint32_t *)(g->datap());
}

extern "C" void set_csr_ptr(const svOpenArrayHandle r) {
  VerilatedDpiOpenVar *g = (VerilatedDpiOpenVar *)r;
  g_csr                  = (uint32_t *)(g->datap());
}

// 加载 hex 文本镜像：每行一个 32 位字（8 个 hex 数字），按低字节在前装入 pmem
static void load_image(const char *path) {
  FILE *f = fopen(path, "r");
  if (!f) {
    fprintf(stderr, "FATAL: cannot open image '%s'\n", path);
    exit(2);
  }
  uint32_t addr = 0;
  char     line[64];
  while (fgets(line, sizeof(line), f)) {
    unsigned int v = 0;
    if (sscanf(line, "%x", &v) == 1) {
      uint32_t off   = addr;
      pmem[off]      = (uint8_t)(v & 0xFF);
      pmem[off + 1]  = (uint8_t)((v >> 8) & 0xFF);
      pmem[off + 2]  = (uint8_t)((v >> 16) & 0xFF);
      pmem[off + 3]  = (uint8_t)((v >> 24) & 0xFF);
      addr += 4;
    }
  }
  fclose(f);
  printf("loaded image '%s' (%u bytes -> 0x%08X)\n", path, addr, CONFIG_MBASE);
}

// 求值一次并记录波形
static void eval(void) {
  dut->eval();
#ifdef CONFIG_WAVE
  tfp->dump(simtime++);
#endif
}

static void step(void) {
  dut->clock = 0;
  eval();
  dut->clock = 1;
  eval();
  cycle++;
}

int main(int argc, char **argv) {
  Verilated::commandArgs(argc, argv);
  if (argc < 2) {
    fprintf(stderr, "usage: %s <image.hex>\n", argv[0]);
    return 2;
  }

  dut = new VSimTop;
  memset(pmem, 0, sizeof(pmem));
  load_image(argv[1]);

#ifdef CONFIG_WAVE
  Verilated::traceEverOn(true);
  tfp = new VerilatedVcdC;
  dut->trace(tfp, 99);
  tfp->open("wave.vcd");
#endif

  // 复位：异步断言，同步释放
  dut->reset = 1;
  dut->clock = 0;
  eval();
  dut->clock = 1;
  eval();
  dut->reset = 0;
  dut->clock = 0;
  eval();

  while (!g_halted && cycle < CONFIG_MAX_CYCLES) {
    step();
  }

#ifdef CONFIG_WAVE
  tfp->close();
#endif
  dut->final();
  delete dut;

  if (!g_halted) {
    printf("RESULT: TIMEOUT after %llu cycles\n", (unsigned long long)cycle);
    return 1;
  }
  printf("RESULT: tohost=%d at cycle %llu\n", g_tohost, (unsigned long long)cycle);
  return (g_tohost == 1) ? 0 : 1;
}