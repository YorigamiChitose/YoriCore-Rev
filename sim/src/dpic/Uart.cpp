#include <cstdio>

extern "C" void sim_uart_tx(int ch) {
  putchar(ch & 0xff);
  fflush(stdout);
}
