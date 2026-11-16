# ===============================================================
#  Verilator 仿真
# ===============================================================

SIM_TOP       := SimTop
SIM_SRC_DIR   := $(TOP_DIR)/sim/src
SIM_INC_DIR   := $(TOP_DIR)/sim/inc
SIM_BUILD_DIR := $(BUILD_DIR)/verilator
SIM_BIN       := $(SIM_BUILD_DIR)/V$(SIM_TOP)
SIM_SV        := $(OUT_DIR)/$(SIM_TOP).sv
SIM_CSRC      := $(shell find $(SIM_SRC_DIR) -name '*.cpp' 2>/dev/null)
SIM_RSRC      := $(wildcard $(TOP_DIR)/resources/*.sv)
SIM_HDRS      := $(shell find $(SIM_INC_DIR) -name '*.h' 2>/dev/null) $(AUTOCONFIG_H)

# 只有打开 CONFIG_WAVE 才生成 trace 代码（trace 会显著拖慢编译）
WAVE_ENABLED := $(shell grep -q '^CONFIG_WAVE=y' $(CONFIG_FILE) 2>/dev/null && echo 1)
TRACE_FLAGS  := $(if $(WAVE_ENABLED),--trace,)

# DEBUG=1 才带 -g；默认面向快速迭代
DEBUG ?= 0
OPT   ?= -O1

VERILATOR_FLAGS := -MMD -cc -O3 --x-assign fast --x-initial fast --noassert $(TRACE_FLAGS) --output-split 20000 --output-split-cfuncs 4000 --exe -Wno-fatal -Wno-lint
CXXFLAGS := -CFLAGS "-I$(SIM_INC_DIR) -I$(SIM_INC_DIR)/autoconf $(OPT) $(if $(filter 1,$(DEBUG)),-g -fno-var-tracking-assignments,) -Wall -Wno-unused-parameter"
LDFLAGS  := -LDFLAGS -lreadline

IMG ?=
ARGS ?=

.PHONY: sim run difftest

$(SIM_BIN): $(SIM_SV) $(SIM_CSRC) $(SIM_HDRS) $(SIM_RSRC) $(AUTOCONFIG_H) $(TOP_DIR)/sim/verilator.vlt
	$(call log_start,Verilator 编译 $(SIM_TOP))
	@mkdir -p $(SIM_BUILD_DIR)
	verilator $(VERILATOR_FLAGS) --top-module $(SIM_TOP) \
		$(TOP_DIR)/sim/verilator.vlt \
		$(SIM_CSRC) $(OUT_DIR)/*.sv $(SIM_RSRC) \
		$(CXXFLAGS) $(LDFLAGS) -Mdir $(SIM_BUILD_DIR)
	@make -C $(SIM_BUILD_DIR) -f V$(SIM_TOP).mk -j$(shell nproc) -s
	$(call log_done,仿真构建完成 -> $(SIM_BIN))

sim: $(SIM_BIN)

run: $(SIM_BIN)
	@test -n "$(IMG)" || { echo "usage: make run IMG=<image>"; exit 1; }
	@$(SIM_BIN) $(ARGS) $(IMG)

difftest: $(SIM_BIN) ref
	@test -n "$(IMG)" || { echo "usage: make difftest IMG=<image>"; exit 1; }
	@$(SIM_BIN) -b --diff=$(BUILD_DIR)/ref/riscv32-spike-so $(ARGS) $(IMG)
