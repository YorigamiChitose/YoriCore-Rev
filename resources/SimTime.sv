module SimTime #(
  parameter integer TIME_WIDTH = 64
)(
  input         clock,
  input         reset,
  output [TIME_WIDTH-1:0] rtcTime
);
  import "DPI-C" function int sim_time_lo();
  import "DPI-C" function int sim_time_hi();

  reg [TIME_WIDTH-1:0] time_q;

  always @(posedge clock) begin
    if (reset) time_q <= {TIME_WIDTH{1'b0}};
    else time_q <= {sim_time_hi(), sim_time_lo()};
  end

  assign rtcTime = time_q;
endmodule
