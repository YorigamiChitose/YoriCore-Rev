# ===============================================================
#  Spike 参考模型 .so
# ===============================================================

SPIKE_REPO  ?= $(HOME)/yori-riscv-util/riscv-gnu-toolchain/spike
SPIKE_BUILD ?= $(SPIKE_REPO)

.PHONY: ref ref-clean

ref: $(AUTOCONFIG_H)
	$(MAKE) -C $(TOP_DIR)/sim/ref/spike TOP_DIR=$(TOP_DIR) \
		SPIKE_REPO=$(SPIKE_REPO) SPIKE_BUILD=$(SPIKE_BUILD)

ref-clean:
	$(MAKE) -C $(TOP_DIR)/sim/ref/spike TOP_DIR=$(TOP_DIR) \
		SPIKE_REPO=$(SPIKE_REPO) SPIKE_BUILD=$(SPIKE_BUILD) clean
