# ===============================================================
#  定向测试：编译 tests/*.S -> build/tests/*.bin，运行 / 清理
#
#  所有 .S 汇编测试统一编译为裸机镜像，由仿真端加载执行。
#  每个测试的预期结束值（a0）由 tests/expect.txt 统一管理，
#  运行后对比实际 a0 输出 PASS/FAIL 及结果值。
# ===============================================================

TEST_SRC_DIR := $(TOP_DIR)/tests

CROSS_COMPILE ?= riscv64-unknown-elf-
CC            := $(CROSS_COMPILE)gcc
OBJCOPY       := $(CROSS_COMPILE)objcopy

MARCH := rv32ima_zicsr_zifencei
MABI  := ilp32
TEXT  := 0x80000000
CFLAGS := -march=$(MARCH) -mabi=$(MABI) -nostdlib -nostartfiles -Ttext=$(TEXT)

TEST_SRC := $(wildcard $(TEST_SRC_DIR)/*.S)
TEST_BIN := $(patsubst $(TEST_SRC_DIR)/%.S,$(TEST_BUILD_DIR)/%.bin,$(TEST_SRC))

# 统一清单：<测试名> <预期 a0>
TEST_EXPECT := $(TEST_SRC_DIR)/expect.txt

.PHONY: test test-build test-clean

$(TEST_BUILD_DIR):
	@mkdir -p $@

$(TEST_BUILD_DIR)/%.elf: $(TEST_SRC_DIR)/%.S | $(TEST_BUILD_DIR)
	$(CC) $(CFLAGS) -o $@ $<

$(TEST_BUILD_DIR)/%.bin: $(TEST_BUILD_DIR)/%.elf
	$(OBJCOPY) -O binary $< $@

test-build: $(TEST_BIN)

# 运行全部定向测试：按 expect.txt 的预期 a0 判 PASS/FAIL，并输出实际结果值
test: $(SIM_BIN) test-build $(TEST_EXPECT)
	@printf '$(C_RED)--- 定向测试 ---$(C_OFF)\n'
	@pass=0; fail=0; \
	while read name exp rest; do \
	  case "$$name" in \#*|"") continue;; esac; \
	  b=$(TEST_BUILD_DIR)/$$name.bin; \
	  out=$$(env -u LD_LIBRARY_PATH $(SIM_BIN) $$b 2>&1); \
	  val=$$(printf '%s\n' "$$out" | sed -n 's/.*a0=\([0-9][0-9]*\).*/\1/p' | head -1); \
	  if echo "$$out" | grep -q TIMEOUT; then \
	    printf '$(C_RED)FAIL$(C_OFF)  %s (TIMEOUT exp=%s)\n' "$$name" "$$exp"; fail=$$((fail+1)); \
	  elif [ -z "$$val" ]; then \
	    printf '$(C_RED)FAIL$(C_OFF)  %s (no result exp=%s)\n' "$$name" "$$exp"; fail=$$((fail+1)); \
	  elif [ "$$val" = "$$exp" ]; then \
	    printf '$(C_GRN)PASS$(C_OFF)  %s (a0=%s)\n' "$$name" "$$val"; pass=$$((pass+1)); \
	  else \
	    printf '$(C_RED)FAIL$(C_OFF)  %s (a0=%s exp=%s)\n' "$$name" "$$val" "$$exp"; fail=$$((fail+1)); \
	  fi; \
	done < $(TEST_EXPECT); \
	echo "----"; echo "pass=$$pass fail=$$fail"; \
	test $$fail -eq 0

test-clean:
	rm -rf $(TEST_BUILD_DIR)
