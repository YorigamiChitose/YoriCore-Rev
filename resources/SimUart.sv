module SimUart (
  input      clock,
  input      reset,
  input      tx_valid,
  input [7:0] tx_data
);
  import "DPI-C" function void sim_uart_tx(input int ch);

  always @(posedge clock) begin
    if (!reset && tx_valid) sim_uart_tx(tx_data);
  end
endmodule
