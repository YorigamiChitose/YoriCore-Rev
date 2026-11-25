module SimTime (
  input         clock,
  input         reset,
  output [63:0] rtcTime
);
  import "DPI-C" function int sim_time_lo();
  import "DPI-C" function int sim_time_hi();

  reg [63:0] time_q;

  always @(posedge clock) begin
    if (reset) time_q <= 64'd0;
    else time_q <= {sim_time_hi(), sim_time_lo()};
  end

  assign rtcTime = time_q;
endmodule
