#!/usr/bin/env bash
# 回归测试：读取 tests/regression.list，逐个运行仿真并核对退出码。
# 列表格式：image.hex:expected_exit（0=pass，非0=fail）
set -u

BIN="${BIN:-./build/verilator/VSimTop}"
LIST="${1:-tests/regression.list}"
TESTS_DIR="$(dirname "$LIST")"

pass=0
fail=0

while IFS=':' read -r img exp || [ -n "$img" ]; do
  # 去掉空白与注释，跳过空行
  img="${img%%[[:space:]]*}"
  exp="${exp%%[[:space:]]*}"
  [ -z "$img" ] && continue
  case "$img" in \#*) continue ;; esac

  env -u LD_LIBRARY_PATH "$BIN" "$TESTS_DIR/$img" >/dev/null 2>&1
  got=$?

  if [ "$got" = "$exp" ]; then
    printf 'PASS  %s\n' "$img"
    pass=$((pass + 1))
  else
    printf 'FAIL  %s (expected %s, got %s)\n' "$img" "$exp" "$got"
    fail=$((fail + 1))
  fi
done < "$LIST"

printf -- '----\n'
printf 'pass=%d fail=%d\n' "$pass" "$fail"
[ "$fail" -eq 0 ]