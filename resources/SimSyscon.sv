module SimSyscon (
  input      clock,
  input      reset,
  input      exit_valid,
  input      exit_reboot
);
  import "DPI-C" function void sim_syscon(input int reboot);

  always @(posedge clock) begin
    if (!reset && exit_valid) sim_syscon(exit_reboot);
  end
endmodule
