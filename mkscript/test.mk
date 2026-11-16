# ===============================================================
#  定向测试：编译 tests/*.S -> build/tests/*.bin，运行 / 清理
#
#  所有 .S 汇编测试统一编译为裸机镜像，由仿真端加载执行。
#  文件名以 _fail 结尾者为负向测试（预期退出码 1），其余预期 0。
# ===============================================================

TEST_SRC_DIR := $(TOP_DIR)/tests

CROSS_COMPILE ?= riscv64-unknown-elf-
CC            := $(CROSS_COMPILE)gcc
OBJCOPY       := $(CROSS_COMPILE)objcopy

MARCH := rv32ima
MABI  := ilp32
TEXT  := 0x80000000
CFLAGS := -march=$(MARCH) -mabi=$(MABI) -nostdlib -nostartfiles -Ttext=$(TEXT)

TEST_SRC := $(wildcard $(TEST_SRC_DIR)/*.S)
TEST_BIN := $(patsubst $(TEST_SRC_DIR)/%.S,$(TEST_BUILD_DIR)/%.bin,$(TEST_SRC))

.PHONY: test test-build test-clean

$(TEST_BUILD_DIR):
	@mkdir -p $@

$(TEST_BUILD_DIR)/%.elf: $(TEST_SRC_DIR)/%.S | $(TEST_BUILD_DIR)
	$(CC) $(CFLAGS) -o $@ $<

$(TEST_BUILD_DIR)/%.bin: $(TEST_BUILD_DIR)/%.elf
	$(OBJCOPY) -O binary $< $@

test-build: $(TEST_BIN)

# 运行全部定向测试：_fail 后缀预期退出码 1，其余预期 0，退出码 2 记超时
test: $(SIM_BIN) test-build
	@printf '$(C_RED)--- 定向测试 ---$(C_OFF)\n'
	@pass=0; fail=0; timeout=0; \
	for b in $(TEST_BIN); do \
	  name=$$(basename $$b .bin); \
	  case "$$name" in *_fail) exp=1;; *) exp=0;; esac; \
	  env -u LD_LIBRARY_PATH $(SIM_BIN) $$b >/dev/null 2>&1; rc=$$?; \
	  if [ $$rc -eq $$exp ]; then echo "PASS  $$name"; pass=$$((pass+1)); \
	  elif [ $$rc -eq 2 ]; then echo "TIME  $$name"; timeout=$$((timeout+1)); \
	  else echo "FAIL  $$name (rc=$$rc exp=$$exp)"; fail=$$((fail+1)); fi; \
	done; \
	echo "----"; echo "pass=$$pass fail=$$fail timeout=$$timeout"; \
	test $$fail -eq 0

test-clean:
	rm -rf $(TEST_BUILD_DIR)
