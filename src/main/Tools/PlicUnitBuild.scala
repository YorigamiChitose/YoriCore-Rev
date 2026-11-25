package Tools

object PlicUnitBuild extends App {
  circt.stage.ChiselStage.emitSystemVerilogFile(new PlicUnitTop, args, Array("--disable-all-randomization"))
}
