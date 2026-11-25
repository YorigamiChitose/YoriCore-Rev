#include "utils.h"

extern "C" int sim_time_lo() { return (int)(get_time() & 0xffffffffu); }
extern "C" int sim_time_hi() { return (int)(get_time() >> 32); }
