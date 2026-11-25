package Tools

object UartUnitBuild extends App {
  circt.stage.ChiselStage.emitSystemVerilogFile(new UartUnitTop, args, Array("--disable-all-randomization"))
}
