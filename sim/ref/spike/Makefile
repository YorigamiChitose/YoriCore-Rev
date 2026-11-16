# Spike difftest 参考库（.so）
# 使用外部已构建的 Spike 树，不把 Spike 源码放进本仓库。

TOP_DIR    ?= $(abspath ../../..)
SPIKE_REPO ?= $(HOME)/yori-riscv-util/riscv-gnu-toolchain/spike
SPIKE_BUILD ?= $(SPIKE_REPO)
BUILD_DIR  ?= $(TOP_DIR)/build/ref
TARGET     := $(BUILD_DIR)/riscv32-spike-so
SRCS       := $(TOP_DIR)/sim/ref/spike/difftest.cc

INC := -I$(SPIKE_REPO) \
       -I$(SPIKE_REPO)/fesvr \
       -I$(SPIKE_REPO)/riscv \
       -I$(SPIKE_REPO)/disasm \
       -I$(SPIKE_REPO)/softfloat \
       -I$(SPIKE_REPO)/spike_main \
       -I$(SPIKE_REPO)/spike_dasm \
       -I$(SPIKE_REPO)/fdt \
       -I$(SPIKE_REPO)/customext \
       -I$(TOP_DIR)/sim/inc

LIBS := $(SPIKE_BUILD)/libspike_main.a \
        $(SPIKE_BUILD)/libriscv.a \
        $(SPIKE_BUILD)/libdisasm.a \
        $(SPIKE_BUILD)/libsoftfloat.a \
        $(SPIKE_BUILD)/libfesvr.a \
        $(SPIKE_BUILD)/libfdt.a

.PHONY: all clean check

all: $(TARGET)

$(TARGET): $(SRCS) $(TOP_DIR)/sim/inc/autoconf/autoconf.h
	@test -f $(SPIKE_BUILD)/libriscv.a || { \
	  echo "Spike static libs not found in SPIKE_BUILD=$(SPIKE_BUILD)"; \
	  echo "Set SPIKE_REPO to a built Spike tree (e.g. ~/yori-riscv-util/riscv-gnu-toolchain/spike)."; \
	  exit 1; }
	@mkdir -p $(BUILD_DIR)
	g++ -std=c++20 -O2 -shared -fPIC -fvisibility=hidden $(INC) $(SRCS) $(LIBS) -lpthread -o $@

check:
	@test -f $(TARGET) && echo "reference: $(TARGET)" || echo "reference not built"

clean:
	rm -f $(TARGET)
