// SimCommit：提交级观测（difftest 用黑盒）。
// 仅在 SimTop 中、且 Config.Sim.Enable 时实例化；Core/Top 不包含本模块。
module SimCommit #(
  parameter integer ADDR_WIDTH = 32
)(
  input                    clock,
  input                    reset,
  input                    valid,
  input  [ADDR_WIDTH-1:0]  pc,
  input  [ADDR_WIDTH-1:0]  npc,
  input  [31:0]            inst,
  input  [1:0]             priv,
  input  [3:0]             exc,
  input                    rdEn,
  input  [4:0]             rdAddr,
  input  [31:0]            rdData,
  input                    csrEn,
  input  [11:0]            csrAddr,
  input  [31:0]            csrData,
  input                    memValid,
  input                    memWrite,
  input  [ADDR_WIDTH-1:0]  memAddr,
  input  [31:0]            memWdata,
  input  [1:0]             memSize
);
  import "DPI-C" function void sim_commit(
    input int pc, input int npc, input int inst, input int priv, input int exc,
    input int rd_en, input int rd_addr, input int rd_data,
    input int csr_en, input int csr_addr, input int csr_data,
    input int mem_valid, input int mem_write, input int mem_addr,
    input int mem_wdata, input int mem_size);

  always @(posedge clock) begin
    if (!reset && valid)
      sim_commit(pc, npc, inst, priv, exc,
                 rdEn, rdAddr, rdData,
                 csrEn, csrAddr, csrData,
                 memValid, memWrite, memAddr, memWdata, memSize);
  end
endmodule
