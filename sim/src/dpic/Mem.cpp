#include "memory/memory.h"

extern "C" int dpic_mem_read(int addr) {
  uint32_t a = (uint32_t)addr;
  if (in_pmem(a)) return (int)pmem_read(a, 4);
  return 0;
}

extern "C" void dpic_mem_write(int addr, int data, int strb) {
  uint32_t a = (uint32_t)addr;
  if (in_pmem(a)) {
    for (int i = 0; i < 4; i++)
      if (strb & (1 << i)) *(guest_to_host(a + i)) = (uint8_t)((unsigned)data >> (8 * i));
  }
}
