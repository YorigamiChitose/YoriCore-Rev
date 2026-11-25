package Tools

object DeviceUnitBuild extends App {
  val firtoolLoweringOptions = Array(
    "--lowering-options=" + List(
      "disallowLocalVariables",
      "disallowPackedArrays",
      "locationInfoStyle=none"
    ).reduce(_ + "," + _)
  )
  val firtoolNormalOptions = Array("--disable-all-randomization")
  val firtoolOptions       = firtoolNormalOptions ++ firtoolLoweringOptions

  circt.stage.ChiselStage.emitSystemVerilogFile(new DeviceUnitTop, args, firtoolOptions)
}