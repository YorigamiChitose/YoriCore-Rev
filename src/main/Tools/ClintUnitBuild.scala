package Tools

object ClintUnitBuild extends App {
  circt.stage.ChiselStage.emitSystemVerilogFile(new ClintUnitTop, args, Array("--disable-all-randomization"))
}
