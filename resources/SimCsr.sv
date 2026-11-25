// SimCsr：CSR 实际状态镜像（CLI 用黑盒）。
// 仅在 SimTop 中、且 Config.Sim.Enable 时实例化。
module SimCsr (
  input        clock,
  input        reset,
  input [31:0] mepc,
  input [31:0] mcause,
  input [31:0] mstatus,
  input [31:0] mtvec,
  input [1:0]  priv,
  input [31:0] sepc,
  input [31:0] scause,
  input [31:0] stvec,
  input [31:0] sie,
  input [31:0] sip,
  input [31:0] sstatus,
  input [31:0] satp
);
  import "DPI-C" function void sim_csr_update(
    input int mepc, input int mcause, input int mstatus, input int mtvec, input int priv,
    input int sepc, input int scause, input int stvec, input int sie,
    input int sip, input int sstatus, input int satp);

  always @(negedge clock) begin
    if (!reset)
      sim_csr_update(mepc, mcause, mstatus, mtvec, priv,
                     sepc, scause, stvec, sie, sip, sstatus, satp);
  end
endmodule
