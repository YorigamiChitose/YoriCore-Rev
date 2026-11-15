// SimAXIRam：AXI4 从设备存储模型（仿真用黑盒）。
// 端口名与 Chisel 黑盒实例匹配（无 io_ 前缀）。
// 实现 AW/W/B/AR/R 五通道握手，支持突发（行访问 4 拍）与单拍（MMIO 直通），
// 数据按 beat 通过 DPI-C 转发给 C++ 的 pmem。
// LATENCY：读/写响应延迟（额外等待周期），0 为最小 ~2 拍。
module SimAXIRam #(
  parameter integer ADDR_WIDTH = 32,
  parameter integer LATENCY    = 0
)(
  input                    clock,
  input                    reset,

  input                    axi_aw_valid,
  output                   axi_aw_ready,
  input             [0:0]  axi_aw_bits_id,
  input  [ADDR_WIDTH-1:0] axi_aw_bits_addr,
  input             [7:0] axi_aw_bits_len,
  input             [2:0] axi_aw_bits_size,
  input             [1:0] axi_aw_bits_burst,
  input             [3:0] axi_aw_bits_cache,
  input             [2:0] axi_aw_bits_prot,

  input                    axi_w_valid,
  output                   axi_w_ready,
  input            [63:0]  axi_w_bits_data,
  input             [7:0]  axi_w_bits_strb,
  input                    axi_w_bits_last,

  output                   axi_b_valid,
  input                    axi_b_ready,
  output            [0:0]  axi_b_bits_id,
  output            [1:0]  axi_b_bits_resp,

  input                    axi_ar_valid,
  output                   axi_ar_ready,
  input             [0:0]  axi_ar_bits_id,
  input  [ADDR_WIDTH-1:0] axi_ar_bits_addr,
  input             [7:0] axi_ar_bits_len,
  input             [2:0] axi_ar_bits_size,
  input             [1:0] axi_ar_bits_burst,
  input             [3:0] axi_ar_bits_cache,
  input             [2:0] axi_ar_bits_prot,

  output                   axi_r_valid,
  input                    axi_r_ready,
  output            [0:0]  axi_r_bits_id,
  output           [63:0]  axi_r_bits_data,
  output            [1:0]  axi_r_bits_resp,
  output                   axi_r_bits_last
);
  import "DPI-C" function int  dpic_mem_read (input int addr);
  import "DPI-C" function void dpic_mem_write(input int addr, input int data, input int strb);

  localparam S_IDLE = 3'd0, S_RD_WAIT = 3'd1, S_RD = 3'd2,
             S_WR = 3'd3, S_B_WAIT = 3'd4, S_B = 3'd5;
  reg [2:0] state;

  reg [ADDR_WIDTH-1:0] r_addr;
  reg             [7:0] r_len;
  reg             [7:0] r_cnt;
  reg             [0:0] r_id;
  reg             [7:0] w_cnt;
  reg            [63:0] rdata;
  reg             [7:0] lat_cnt;

  always @(posedge clock) begin
    if (reset) begin
      state   <= S_IDLE;
      r_addr  <= 0;
      r_len   <= 0;
      r_cnt   <= 0;
      r_id    <= 0;
      w_cnt   <= 0;
      rdata   <= 0;
      lat_cnt <= 0;
    end else begin
      case (state)
        S_IDLE: begin
          if (axi_ar_valid && axi_ar_ready) begin
            r_addr  <= axi_ar_bits_addr;
            r_len   <= axi_ar_bits_len;
            r_cnt   <= 0;
            r_id    <= axi_ar_bits_id;
            lat_cnt <= 0;
            state   <= S_RD_WAIT;
          end else if (axi_aw_valid && axi_aw_ready) begin
            r_addr <= axi_aw_bits_addr;
            r_len  <= axi_aw_bits_len;
            w_cnt  <= 0;
            state  <= S_WR;
          end
        end
        S_RD_WAIT: begin
          if (lat_cnt >= LATENCY) begin
            // 首拍数据（8 字节：低字 + 高字）
            rdata <= {dpic_mem_read(r_addr + 4), dpic_mem_read(r_addr)};
            state <= S_RD;
          end else begin
            lat_cnt <= lat_cnt + 1;
          end
        end
        S_RD: begin
          if (axi_r_valid && axi_r_ready) begin
            if (r_cnt == r_len) begin
              state <= S_IDLE;
            end else begin
              r_cnt <= r_cnt + 1;
              rdata <= {dpic_mem_read(r_addr + (r_cnt + 1) * 8 + 4),
                        dpic_mem_read(r_addr + (r_cnt + 1) * 8)};
            end
          end
        end
        S_WR: begin
          if (axi_w_valid && axi_w_ready) begin
            dpic_mem_write(r_addr + w_cnt * 8,     axi_w_bits_data[31:0],  axi_w_bits_strb[3:0]);
            dpic_mem_write(r_addr + w_cnt * 8 + 4, axi_w_bits_data[63:32], axi_w_bits_strb[7:4]);
            w_cnt <= w_cnt + 1;
            if (axi_w_bits_last) begin
              lat_cnt <= 0;
              state   <= S_B_WAIT;
            end
          end
        end
        S_B_WAIT: begin
          if (lat_cnt >= LATENCY) state <= S_B;
          else lat_cnt <= lat_cnt + 1;
        end
        S_B: begin
          if (axi_b_valid && axi_b_ready) state <= S_IDLE;
        end
        default: state <= S_IDLE;
      endcase
    end
  end

  assign axi_ar_ready = (state == S_IDLE);
  assign axi_aw_ready = (state == S_IDLE);

  assign axi_r_valid       = (state == S_RD);
  assign axi_r_bits_id     = r_id;
  assign axi_r_bits_data   = rdata;
  assign axi_r_bits_resp   = 2'b00;
  assign axi_r_bits_last   = (r_cnt == r_len);

  assign axi_w_ready = (state == S_WR);

  assign axi_b_valid     = (state == S_B);
  assign axi_b_bits_id   = 1'b0;
  assign axi_b_bits_resp = 2'b00;
endmodule