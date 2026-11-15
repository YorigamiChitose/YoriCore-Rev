#!/usr/bin/env bash
# 运行 ysyx AM cpu-tests 批量回归（外部预编译 rv32npc 二进制）
# 退出码约定：0=pass，1=ebreak a0!=0 判 fail，2=超时
set -u

BIN="${BIN:-./build/verilator/VSimTop}"
BUILD_DIR="${1:-/home/yori/ysyx-workbench/am-kernels/tests/cpu-tests/build}"
MAX_CYCLES="${MAX_CYCLES:-20000}"

pass=0
fail=0
timeout=0

for bin in "$BUILD_DIR"/*-riscv32-npc.bin; do
  [ -e "$bin" ] || continue
  name=$(basename "$bin")
  name=${name%-riscv32-npc.bin}

  env -u LD_LIBRARY_PATH "$BIN" "$bin" "$MAX_CYCLES" >/dev/null 2>&1
  rc=$?

  case "$name" in
    wrong)
      # wrong 是「必须失败」的测试，退出码 1 视为正确
      if [ "$rc" -eq 1 ]; then printf 'PASS  %s (expected fail)\n' "$name"; pass=$((pass + 1));
      elif [ "$rc" -eq 2 ]; then printf 'TIME %s\n' "$name"; timeout=$((timeout + 1));
      else printf 'FAIL  %s (unexpected rc=%d)\n' "$name" "$rc"; fail=$((fail + 1)); fi
      ;;
    *)
      if [ "$rc" -eq 0 ]; then printf 'PASS  %s\n' "$name"; pass=$((pass + 1));
      elif [ "$rc" -eq 2 ]; then printf 'TIME %s\n' "$name"; timeout=$((timeout + 1));
      else printf 'FAIL  %s (rc=%d)\n' "$name" "$rc"; fail=$((fail + 1)); fi
      ;;
  esac
done

printf -- '----\n'
printf 'pass=%d fail=%d timeout=%d\n' "$pass" "$fail" "$timeout"