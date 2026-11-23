module SimPerf (
  input        clock,
  input        reset,
  input        valid,
  input  [31:0] inst,
  input        iHit,
  input        iMiss,
  input        dHit,
  input        dMiss
);
  import "DPI-C" function void sim_perf_update(
    input int cycles_lo, input int cycles_hi,
    input int instret_lo, input int instret_hi,
    input int i_hit_lo, input int i_hit_hi,
    input int i_miss_lo, input int i_miss_hi,
    input int d_hit_lo, input int d_hit_hi,
    input int d_miss_lo, input int d_miss_hi,
    input int fence_lo, input int fence_hi,
    input int fence_i_lo, input int fence_i_hi,
    input int wfi_lo, input int wfi_hi);

  reg [63:0] cycles, instret, i_hit, i_miss, d_hit, d_miss;
  reg [63:0] fence_cnt, fence_i_cnt, wfi_cnt;

  wire is_fence   = valid && (inst == 32'h0ff0000f);
  wire is_fence_i = valid && (inst == 32'h0000100f);
  wire is_wfi     = valid && (inst == 32'h10500073);

  wire [63:0] cycles_s   = cycles + 64'd1;
  wire [63:0] instret_s  = instret + (valid ? 64'd1 : 64'd0);
  wire [63:0] i_hit_s    = i_hit + (iHit ? 64'd1 : 64'd0);
  wire [63:0] i_miss_s   = i_miss + (iMiss ? 64'd1 : 64'd0);
  wire [63:0] d_hit_s    = d_hit + (dHit ? 64'd1 : 64'd0);
  wire [63:0] d_miss_s   = d_miss + (dMiss ? 64'd1 : 64'd0);
  wire [63:0] fence_s    = fence_cnt + (is_fence ? 64'd1 : 64'd0);
  wire [63:0] fence_i_s  = fence_i_cnt + (is_fence_i ? 64'd1 : 64'd0);
  wire [63:0] wfi_s      = wfi_cnt + (is_wfi ? 64'd1 : 64'd0);

  always @(posedge clock) begin
    if (reset) begin
      cycles <= 64'd0;
      instret <= 64'd0;
      i_hit <= 64'd0;
      i_miss <= 64'd0;
      d_hit <= 64'd0;
      d_miss <= 64'd0;
      fence_cnt <= 64'd0;
      fence_i_cnt <= 64'd0;
      wfi_cnt <= 64'd0;
    end else begin
      cycles <= cycles_s;
      instret <= instret_s;
      i_hit <= i_hit_s;
      i_miss <= i_miss_s;
      d_hit <= d_hit_s;
      d_miss <= d_miss_s;
      fence_cnt <= fence_s;
      fence_i_cnt <= fence_i_s;
      wfi_cnt <= wfi_s;

      if (valid)
        sim_perf_update(
          cycles_s[31:0], cycles_s[63:32],
          instret_s[31:0], instret_s[63:32],
          i_hit_s[31:0], i_hit_s[63:32],
          i_miss_s[31:0], i_miss_s[63:32],
          d_hit_s[31:0], d_hit_s[63:32],
          d_miss_s[31:0], d_miss_s[63:32],
          fence_s[31:0], fence_s[63:32],
          fence_i_s[31:0], fence_i_s[63:32],
          wfi_s[31:0], wfi_s[63:32]);
    end
  end
endmodule
