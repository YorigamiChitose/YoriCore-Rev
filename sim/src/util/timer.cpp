#include <cstdint>
#include <cstdlib>
#include <sys/time.h>

uint64_t get_time_internal(void) {
  struct timeval now;
  gettimeofday(&now, NULL);
  return now.tv_sec * 1000000 + now.tv_usec;
}

uint64_t get_time(void) { return get_time_internal(); }

void init_rand(void) { srand(get_time_internal()); }
