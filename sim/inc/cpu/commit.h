#ifndef __CPU_COMMIT_H__
#define __CPU_COMMIT_H__

#include "common.h"

struct CommitEvent {
  uint32_t pc;
  uint32_t npc;
  uint32_t inst;
  uint8_t priv;
  uint8_t exc;
  bool rd_en;
  uint8_t rd_addr;
  uint32_t rd_data;
  bool csr_en;
  uint16_t csr_addr;
  uint32_t csr_data;
  bool mem_valid;
  bool mem_write;
  uint32_t mem_addr;
  uint32_t mem_wdata;
  uint8_t mem_size; // 0=B,1=H,2=W
};

void commit_enqueue(const CommitEvent &e);
bool commit_dequeue(CommitEvent &e);

#endif
