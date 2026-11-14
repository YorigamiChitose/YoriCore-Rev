# ===============================================================
#  Verilator 仿真构建（简化版：无 menuconfig/autoconf）
#
#  流程：mill 生成 SimTop.sv -> Verilator 编译 -> 链接 C++ 驱动
# ===============================================================

SIM_TOP       ?= SimTop
SIM_SRC_DIR   := $(TOP_DIR)/sim/src
SIM_INC_DIR   := $(TOP_DIR)/sim/inc
SIM_BUILD_DIR := $(BUILD_DIR)/verilator
SIM_BIN       := $(SIM_BUILD_DIR)/V$(SIM_TOP)

IMG ?= $(TOP_DIR)/tests/smoke.hex

.PHONY: sim run

sim:
	$(call log_start,生成 $(SIM_TOP).sv)
	@mkdir -p $(OUT_DIR)
	mill -i runMain Tools.SimBuild --target-dir $(OUT_DIR) --split-verilog
	$(call log_done,$(SIM_TOP).sv 已生成)
	$(call log_start,Verilator 编译 $(SIM_TOP))
	@mkdir -p $(SIM_BUILD_DIR)
	verilator --cc --exe -Wno-fatal -Wno-lint --top-module $(SIM_TOP) --trace \
		-CFLAGS "-I$(SIM_INC_DIR) -O2" \
		$(SIM_SRC_DIR)/main.cpp \
		$(OUT_DIR)/*.sv \
		$(TOP_DIR)/resources/SimAXIRam.sv \
		-Mdir $(SIM_BUILD_DIR)
	@make -C $(SIM_BUILD_DIR) -f V$(SIM_TOP).mk -j$(shell nproc) -s
	$(call log_done,仿真构建完成 -> $(SIM_BIN))

run: sim
	@printf '$(C_RED)--- 仿真运行 $(subst $(TOP_DIR)/,,$(IMG)) ---$(C_OFF)\n'
	@$(SIM_BIN) $(IMG)