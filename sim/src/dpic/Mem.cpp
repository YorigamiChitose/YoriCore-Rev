// DPI-C：SimAXIRam 的存储访问。
// MMIO 设备（halt/串口等）由 device 模块统一处理；其余地址映射到 pmem。
#include "memory/memory.h"
#include "device/device.h"

extern "C" int dpic_mem_read(int addr) {
  uint32_t a = (uint32_t)addr;
  if (device_is_mmio(a)) return (int)device_read(a);
  if (in_pmem(a)) return (int)pmem_read(a, 4);
  return 0;
}

extern "C" void dpic_mem_write(int addr, int data, int strb) {
  uint32_t a = (uint32_t)addr;
  if (device_is_mmio(a)) {
    device_write(a, (uint32_t)data, (uint8_t)strb);
    return;
  }
  if (in_pmem(a)) {
    for (int i = 0; i < 4; i++)
      if (strb & (1 << i)) *(guest_to_host(a + i)) = (uint8_t)((unsigned)data >> (8 * i));
  }
}
