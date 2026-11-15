# ===============================================================
#  Verilator 仿真构建（增量：仅在源码变化时重建）
# ===============================================================

SIM_TOP       ?= SimTop
SIM_SRC_DIR   := $(TOP_DIR)/sim/src
SIM_INC_DIR   := $(TOP_DIR)/sim/inc
SIM_BUILD_DIR := $(BUILD_DIR)/verilator
SIM_BIN       := $(SIM_BUILD_DIR)/V$(SIM_TOP)
SIM_SV_OUT    := $(OUT_DIR)/$(SIM_TOP).sv

IMG ?= $(TOP_DIR)/tests/smoke.hex

SIM_CSRC := $(shell find $(SIM_SRC_DIR) -name '*.cpp' 2>/dev/null)
SIM_RSRC := $(wildcard $(TOP_DIR)/resources/*.sv)

# 1) 生成 SimTop.sv（依赖 Scala 源码，仅在源码变化时重建）
$(SIM_SV_OUT): $(VERILOG_SRC)
	$(call log_start,生成 $(SIM_TOP).sv)
	@mkdir -p $(OUT_DIR)
	mill -i runMain Tools.SimBuild --target-dir $(OUT_DIR) --split-verilog
	$(call log_done,$(SIM_TOP).sv 已生成)

# 2) Verilator 编译（依赖 SimTop.sv + C++ 源码 + 资源 SV）
$(SIM_BIN): $(SIM_SV_OUT) $(SIM_CSRC) $(SIM_RSRC)
	$(call log_start,Verilator 编译 $(SIM_TOP))
	@mkdir -p $(SIM_BUILD_DIR)
	verilator --cc --exe -Wno-fatal -Wno-lint --top-module $(SIM_TOP) --trace \
		-CFLAGS "-I$(SIM_INC_DIR) -O2" \
		$(SIM_CSRC) \
		$(OUT_DIR)/*.sv \
		$(SIM_RSRC) \
		-Mdir $(SIM_BUILD_DIR)
	@make -C $(SIM_BUILD_DIR) -f V$(SIM_TOP).mk -j$(shell nproc) -s
	$(call log_done,仿真构建完成 -> $(SIM_BIN))

.PHONY: sim run
sim: $(SIM_BIN)

run: $(SIM_BIN)
	@printf '$(C_RED)--- 仿真运行 $(subst $(TOP_DIR)/,,$(IMG)) ---$(C_OFF)\n'
	@env -u LD_LIBRARY_PATH $(SIM_BIN) $(IMG)