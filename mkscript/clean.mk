.PHONY: clean

clean:
	@printf '$(C_RED)--- 清理构建产物 ---$(C_OFF)\n'
	rm -rf $(OUT_DIR) $(SIM_BUILD_DIR) $(BUILD_DIR)/ref $(BUILD_DIR)/tests $(BUILD_DIR)/difftest
