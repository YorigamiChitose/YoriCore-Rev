// DPI-C：SimAXIRam 的存储访问。
// 0xF0000000 是 1-byte halt/tohost 设备；其它地址映射到 pmem。
#include "memory/memory.h"

extern "C" int dpic_mem_read(int addr) {
  uint32_t a = (uint32_t)addr;
  if (a == (uint32_t)CONFIG_HALT_ADDR) return halt_get_value();
  if (in_pmem(a)) return (int)pmem_read(a, 4);
  return 0;
}

extern "C" void dpic_mem_write(int addr, int data, int strb) {
  uint32_t a = (uint32_t)addr;
  if (a == (uint32_t)CONFIG_HALT_ADDR) {
    unsigned s = (unsigned)strb & 0xf;
    if (s == 0) return;
    if (__builtin_popcount(s) != 1) {
      // 非单字节写：由 monitor 在 commit 侧报设备错误
      halt_mark_error();
      return;
    }
    halt_write_byte((unsigned)data);
    return;
  }
  if (in_pmem(a)) {
    for (int i = 0; i < 4; i++)
      if (strb & (1 << i)) *(guest_to_host(a + i)) = (uint8_t)((unsigned)data >> (8 * i));
  }
}
