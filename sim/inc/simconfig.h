#pragma once

// 仿真固定配置（替代 menuconfig/autoconf）
// 需与 src/main/Core/Config/Config.scala 保持一致

#define CONFIG_MBASE     0x80000000u          // 内存基址（= Config.Addr.Base）
#define CONFIG_MSIZE     (16u * 1024u * 1024u) // 16MB 物理内存
#define CONFIG_TOHOST    0xF0000000u          // tohost MMIO 地址（= Config.Mmio.Base）
#define CONFIG_MMIO_BASE 0xF0000000u          // MMIO 基址（>= 此地址不可缓存，直通）
#define CONFIG_MMIO_SIZE (4u * 1024u)         // 4KB MMIO 存储（tohost 位于偏移 0，特殊处理）
#define CONFIG_MAX_CYCLES 2000000ull          // 仿真超时周期数

#define CONFIG_WAVE 1                        // 是否生成 VCD 波形