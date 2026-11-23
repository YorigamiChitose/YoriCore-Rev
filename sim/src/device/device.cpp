// MMIO 设备表与统一分发。
// 设备区间与 RTL 侧 src/main/Core/Config/Config.scala 的 Config.Mmio.Devices 保持一致。
#include "device/device.h"
#include "memory/memory.h"
#include "utils.h"

#include <cstdio>

// ---- 各设备回调（off 为设备内偏移，strb 为字节选通）----

// UART (ns16550)：THR(+0) 写 1 字节 = 发送；LSR(+5) 读 = 0x60（THR empty | TX empty）
static uint32_t uart_read(uint32_t off) {
  return (off == 5) ? 0x60 : 0;
}
static void uart_write(uint32_t off, uint32_t data, uint8_t strb) {
  if (off == 0 && (strb & 0x1)) {
    putchar((int)(data & 0xff));
    fflush(stdout);
  }
}

// RTC：只读 64 位微秒（get_time()），低 32 位 @+0、高 32 位 @+4
static uint32_t rtc_read(uint32_t off) {
  uint64_t us = get_time();
  return (off == 0) ? (uint32_t)(us & 0xffffffffULL) : (uint32_t)(us >> 32);
}
static void rtc_write(uint32_t, uint32_t, uint8_t) {
  // RTC 只读，写操作忽略
}

// halt/tohost：1 字节设备
static uint32_t halt_read(uint32_t off) {
  return (off == 0) ? halt_get_value() : 0;
}
static void halt_write(uint32_t off, uint32_t data, uint8_t strb) {
  if (off != 0) return;
  unsigned s = (unsigned)strb & 0xf;
  if (s == 0) return;
  if (__builtin_popcount(s) != 1) {
    halt_mark_error(); // 非单字节写：由 monitor 在 commit 侧报设备错误
    return;
  }
  halt_write_byte((unsigned)(data & 0xff));
}

// ---- 设备表 ----
struct DeviceRegion {
  uint32_t base;
  uint32_t size;
  uint32_t (*read)(uint32_t off);
  void (*write)(uint32_t off, uint32_t data, uint8_t strb);
};

static const DeviceRegion kDevices[] = {
  { (uint32_t)CONFIG_RTC_ADDR,    0x1000,     rtc_read,  rtc_write  }, // RTC
  { (uint32_t)CONFIG_SERIAL_PORT, 0x1000,     uart_read, uart_write }, // UART
  { (uint32_t)CONFIG_HALT_ADDR,   0x1000,     halt_read, halt_write }, // halt（单字节写，4KB 即可）
};
static const int kDeviceCount = sizeof(kDevices) / sizeof(kDevices[0]);

static int find_device(uint32_t addr) {
  for (int i = 0; i < kDeviceCount; i++)
    if (addr >= kDevices[i].base && addr < kDevices[i].base + kDevices[i].size)
      return i;
  return -1;
}

bool device_is_mmio(uint32_t addr) { return find_device(addr) >= 0; }

uint32_t device_read(uint32_t addr) {
  int i = find_device(addr);
  if (i < 0) return 0;
  return kDevices[i].read(addr - kDevices[i].base);
}

void device_write(uint32_t addr, uint32_t data, uint8_t strb) {
  int i = find_device(addr);
  if (i < 0) return;
  kDevices[i].write(addr - kDevices[i].base, data, strb);
}
