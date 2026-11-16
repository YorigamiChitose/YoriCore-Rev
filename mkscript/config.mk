# ===============================================================
#  Kconfig + Python kconfiglib（ysyx 风格）
# ===============================================================

KCONFIG        := $(TOP_DIR)/Kconfig
DEFCONFIG_FILE := $(TOP_DIR)/defconfig
CONFIG_FILE    := $(TOP_DIR)/.config
AUTOCONFIG_DIR := $(TOP_DIR)/sim/inc/autoconf
AUTOCONFIG_H   := $(AUTOCONFIG_DIR)/autoconf.h

KCONFIG_PY := /usr/lib/python3/dist-packages

$(CONFIG_FILE):
	python3 $(KCONFIG_PY)/defconfig.py $(DEFCONFIG_FILE)

$(AUTOCONFIG_H): $(CONFIG_FILE) $(KCONFIG)
	@mkdir -p $(AUTOCONFIG_DIR)
	python3 $(KCONFIG_PY)/genconfig.py --header-path $@ --config-out $(CONFIG_FILE)

.PHONY: menuconfig defconfig

menuconfig:
	python3 $(KCONFIG_PY)/menuconfig.py
	python3 $(KCONFIG_PY)/genconfig.py --header-path $(AUTOCONFIG_H) --config-out $(CONFIG_FILE)

defconfig:
	python3 $(KCONFIG_PY)/defconfig.py $(DEFCONFIG_FILE)
	python3 $(KCONFIG_PY)/genconfig.py --header-path $(AUTOCONFIG_H) --config-out $(CONFIG_FILE)
