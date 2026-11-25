package Core.Device

import chisel3._

class CoreIrq extends Bundle {
  val msi = Bool()
  val mti = Bool()
  val mei = Bool()
}
