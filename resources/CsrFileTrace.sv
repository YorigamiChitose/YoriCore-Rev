// CsrFileTrace：CSR 镜像黑盒（DPI set_csr_ptr）
module CsrFileTrace #(
  parameter integer CSR_ADDR_WIDTH = 12,
  parameter integer CSR_DATA_WIDTH = 32,
  parameter integer CSR_NUM        = 4096
)(
  input                          clock,
  input                          reset,
  input                          en,
  input  [CSR_ADDR_WIDTH - 1:0]  addr,
  input  [CSR_DATA_WIDTH - 1:0]  data
);
  reg [CSR_DATA_WIDTH - 1:0] CsrFile [0:CSR_NUM - 1];
  import "DPI-C" function void set_csr_ptr(input logic [CSR_DATA_WIDTH - 1:0] a []);
  initial set_csr_ptr(CsrFile);

  integer i;
  always @(posedge clock) begin
    if (reset) begin
      for (i = 0; i < CSR_NUM; i = i + 1) CsrFile[i] <= 0;
    end else if (en) begin
      CsrFile[addr] <= data;
    end
  end
endmodule