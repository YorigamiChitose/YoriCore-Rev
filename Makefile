# ===============================================================
#  YoriCore-Rev 顶层构建入口
# ===============================================================

TOP_DIR   := $(CURDIR)
BUILD_DIR := $(TOP_DIR)/build
SRC_DIR   := $(TOP_DIR)/src

# 纯 RTL / 仿真 Verilog 输出
OUT_DIR := $(BUILD_DIR)/verilog

TOP_MODULE := SimTop

include $(TOP_DIR)/mkscript/env.mk
include $(TOP_DIR)/mkscript/config.mk
include $(TOP_DIR)/mkscript/verilog.mk
include $(TOP_DIR)/mkscript/verilator.mk
include $(TOP_DIR)/mkscript/mill.mk
include $(TOP_DIR)/mkscript/clean.mk

.PHONY: help

help:
	@printf '$(C_BLU)可用目标:$(C_OFF)\n'
	@printf '  $(C_YEL)menuconfig$(C_OFF)  Kconfig 配置\n'
	@printf '  $(C_YEL)defconfig$(C_OFF)   恢复默认配置\n'
	@printf '  $(C_YEL)verilog$(C_OFF)     生成 Verilog（受 Sim.Enable 控制）\n'
	@printf '  $(C_YEL)sim$(C_OFF)         构建 Verilator 仿真\n'
	@printf '  $(C_YEL)ref$(C_OFF)         构建 Spike 参考 .so\n'
	@printf '  $(C_YEL)run$(C_OFF)         运行交互式 monitor（IMG=镜像）\n'
	@printf '  $(C_YEL)fmt$(C_OFF) / $(C_YEL)check$(C_OFF)  Scala 格式\n'
	@printf '  $(C_YEL)clean$(C_OFF)       清理构建产物\n'

.DEFAULT_GOAL := help
