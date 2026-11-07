# ===============================================================
#  Chisel -> Verilog 生成
#
#  产出单一 $OUT_DIR/$(MODULE).sv，并以 Scala 源码为依赖，
#  任一源码变更都会触发重新生成。
# ===============================================================

# 允许命令行覆盖顶层模块 / 生成入口
MODULE ?= $(TOP_MODULE)
TOOL   ?= $(MILL_RUN)

VERILOG_SRC := $(shell find $(SRC_DIR) -name '*.scala' 2>/dev/null)
VERILOG_OUT := $(OUT_DIR)/$(MODULE).sv

.PHONY: verilog

$(VERILOG_OUT): $(VERILOG_SRC)
	$(call log_start,生成 $(MODULE).sv -> $(OUT_DIR))
	@mkdir -p $(OUT_DIR)
	mill -i runMain $(TOOL) --target-dir $(OUT_DIR) --split-verilog
	$(call log_done,$(MODULE).sv 已生成)

verilog: $(VERILOG_OUT)