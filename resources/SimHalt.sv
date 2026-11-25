module SimHalt #(
  parameter integer HALT_CODE_WIDTH = 31
)(
  input        clock,
  input        reset,
  input        halt_valid,
  input [HALT_CODE_WIDTH-1:0] halt_code
);
  import "DPI-C" function void sim_halt(input int code);

  always @(posedge clock) begin
    if (!reset && halt_valid) sim_halt(halt_code);
  end
endmodule
