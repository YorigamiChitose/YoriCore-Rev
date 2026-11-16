// SimCsr：CSR 实际状态镜像（difftest/CLI 用黑盒）。
// 仅在 SimTop 中、且 Config.Sim.Enable 时实例化。
module SimCsr (
  input        clock,
  input        reset,
  input [31:0] mepc,
  input [31:0] mcause,
  input [31:0] mstatus,
  input [31:0] mtvec,
  input [1:0]  priv
);
  import "DPI-C" function void sim_csr_update(
    input int mepc, input int mcause, input int mstatus, input int mtvec, input int priv);

  always @(negedge clock) begin
    if (!reset)
      sim_csr_update(mepc, mcause, mstatus, mtvec, priv);
  end
endmodule
