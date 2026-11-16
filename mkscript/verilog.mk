# ===============================================================
#  Chisel -> Verilog
# ===============================================================

CHISEL_SRC := $(shell find $(SRC_DIR) -name '*.scala' 2>/dev/null)

.PHONY: verilog

$(OUT_DIR)/Top.sv: $(CHISEL_SRC)
	$(call log_start,生成 Top.sv)
	@mkdir -p $(OUT_DIR)
	mill -i runMain Tools.build --target-dir $(OUT_DIR) --split-verilog
	$(call log_done,Top.sv 已生成)

$(OUT_DIR)/SimTop.sv: $(CHISEL_SRC)
	$(call log_start,生成 SimTop.sv)
	@mkdir -p $(OUT_DIR)
	mill -i runMain Tools.SimBuild --target-dir $(OUT_DIR) --split-verilog
	$(call log_done,SimTop.sv 已生成)

verilog: $(OUT_DIR)/Top.sv
