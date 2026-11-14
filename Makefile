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

TOP_MODULE := Top

# Chisel 生成入口（runMain）
MILL_RUN := Tools.build

# 通用宏：颜色、日志、目录工具
include $(TOP_DIR)/mkscript/env.mk

# 构建目标
include $(TOP_DIR)/mkscript/verilog.mk
include $(TOP_DIR)/mkscript/verilator.mk
include $(TOP_DIR)/mkscript/mill.mk
include $(TOP_DIR)/mkscript/clean.mk

.DEFAULT_GOAL := help