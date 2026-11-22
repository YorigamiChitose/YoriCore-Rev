#ifndef __DEVICE_H__
#define __DEVICE_H__

#include <cstdint>

// MMIO 设备统一访问接口。
// 以 (起始地址, 大小) 区间表管理，与普通 pmem 访存分离。
// 设备表见 device.cpp，与 RTL 侧 Config.Mmio.Devices 保持一致（单一契约）。

bool device_is_mmio(uint32_t addr);
uint32_t device_read(uint32_t addr);                             // 4 字节读
void device_write(uint32_t addr, uint32_t data, uint8_t strb);   // strb 字节选通写

#endif
