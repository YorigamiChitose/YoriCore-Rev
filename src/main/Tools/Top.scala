package Tools

import chisel3._
import chisel3.util._

import Core._
import Core.IFU.module.IMemBundle
import Core.EXU.module.DMemBundle

class Top extends Module {
  val core = Module(new Core)

  val ioIMem = IO(Flipped(new IMemBundle))
  val ioDMem = IO(Flipped(new DMemBundle))

  core.ioIMem <> ioIMem
  core.ioDMem <> ioDMem
}