# ===============================================================
#  YoriCore-Rev 顶层构建入口
#
#  职责：定义项目级配置并聚合各子脚本。
#  本仓库为纯 RTL 核心，构建目标仅负责产出 Verilog。
# ===============================================================

TOP_DIR   := $(CURDIR)
BUILD_DIR := $(TOP_DIR)/build
SRC_DIR   := $(TOP_DIR)/src

# Verilog 输出目录
OUT_DIR := $(BUILD_DIR)/verilog

# 测试产物目录
TEST_BUILD_DIR := $(BUILD_DIR)/tests

TOP_MODULE := Top

# Chisel 生成入口（runMain）
MILL_RUN := Tools.build

# 通用宏：颜色、日志、目录工具
include $(TOP_DIR)/mkscript/env.mk

# 构建目标
include $(TOP_DIR)/mkscript/verilog.mk
include $(TOP_DIR)/mkscript/verilator.mk
include $(TOP_DIR)/mkscript/test.mk
include $(TOP_DIR)/mkscript/mill.mk
include $(TOP_DIR)/mkscript/clean.mk

.PHONY: help

help:
	@printf '$(C_BLU)可用目标:$(C_OFF)\n'
	@printf '  $(C_YEL)verilog$(C_OFF)   生成 Verilog 至 $(OUT_DIR)\n'
	@printf '  $(C_YEL)sim$(C_OFF)      构建 Verilator 仿真\n'
	@printf '  $(C_YEL)run$(C_OFF)      运行仿真（IMG=镜像，默认 build/tests/smoke.bin）\n'
	@printf '  $(C_YEL)test$(C_OFF)     编译并运行全部定向测试\n'
	@printf '  $(C_YEL)fmt$(C_OFF)      格式化 Scala 源码\n'
	@printf '  $(C_YEL)check$(C_OFF)    校验源码格式\n'
	@printf '  $(C_YEL)clean$(C_OFF)    清理构建产物\n'

.DEFAULT_GOAL := help