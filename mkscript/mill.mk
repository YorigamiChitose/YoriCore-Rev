# ===============================================================
#  Mill 辅助目标：格式化与帮助
# ===============================================================

.PHONY: help fmt check

help:
	@printf '$(C_BLU)可用目标:$(C_OFF)\n'
	@printf '  $(C_YEL)verilog$(C_OFF)   生成 Verilog 至 $(OUT_DIR)\n'
	@printf '  $(C_YEL)fmt$(C_OFF)      格式化 Scala 源码\n'
	@printf '  $(C_YEL)check$(C_OFF)    校验源码格式\n'
	@printf '  $(C_YEL)clean$(C_OFF)    清理构建产物\n'

fmt:
	mill -i __.reformat

check:
	mill -i __.checkFormat