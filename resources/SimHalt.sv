module SimHalt (
  input        clock,
  input        reset,
  input        halt_valid,
  input [30:0] halt_code
);
  import "DPI-C" function void sim_halt(input int code);

  always @(posedge clock) begin
    if (!reset && halt_valid) sim_halt(halt_code);
  end
endmodule
