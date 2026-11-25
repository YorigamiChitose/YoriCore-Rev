# RTL Device Capabilities

## Address Map

| Device | Base | Size | Interface |
|---|---:|---:|---|
| SYSCON | 0x0010_0000 | 0x1000 | AXI4 / DeviceController |
| RTC | 0x0010_1000 | 0x1000 | AXI4 / DeviceController |
| CLINT | 0x0200_0000 | 0xC000 | AXI4 / DeviceController |
| PLIC | 0x0C00_0000 | 0x400000 | AXI4 / DeviceController |
| UART | 0x1000_0000 | 0x1000 | AXI4 / DeviceController |
| HALT | 0xF000_0000 | 0x1000 | AXI4 / DeviceController |
| RAM | 0x8000_0000 | 0x1000_0000 | AXI4 |

## SYSCON

Implemented:

- write `0x5555`: poweroff;
- write `0x7777`: reboot.

Not implemented:

- reset reason;
- boot configuration;
- clock/power control.

## RTC

Implemented:

- 64-bit real-time value from the simulation platform;
- low/high word reads at offsets `0` and `4`.

Not implemented:

- alarm;
- interrupt;
- write/control registers;
- calendar conversion in hardware.

Current unit is microseconds.

## CLINT

Implemented:

- `msip`;
- `mtimecmp` low/high;
- `mtime` low/high;
- `msi` / `mti` outputs.

Current `mtime` is an RTL cycle counter.

Not implemented:

- software write to `mtime`;
- platform-configurable timer frequency;
- multiple harts.

## PLIC

Implemented:

- 32 sources;
- 2 contexts;
- priority;
- enable;
- threshold;
- pending;
- claim;
- complete;
- basic gateway blocking while a source remains claimed.

Not implemented:

- 1024-source configuration;
- full PLIC gateway timing model;
- per-context source routing beyond the current two contexts;
- interrupt source aggregation for arbitrary devices.

## UART

Implemented:

- THR write;
- LSR read;
- IER;
- IIR;
- TX interrupt pending;
- TX output through the simulation platform.

Not implemented:

- RX;
- RX FIFO;
- TX FIFO;
- baud-rate generation;
- modem control;
- parity/framing errors;
- flow control.

## HALT

Implemented:

- one-byte write;
- `(code << 1) | 1` protocol;
- exit notification through the simulation platform.

Not implemented:

- multi-byte access;
- source-specific exit metadata.

## Platform Boundary

The C++ simulation platform provides only:

- RAM model;
- RTC time source;
- UART output sink;
- HALT/SYSCON exit handling;
- performance counters;
- monitor/debug display.

All MMIO devices are RTL and are reached through the AXI4 interconnect and DeviceController.
