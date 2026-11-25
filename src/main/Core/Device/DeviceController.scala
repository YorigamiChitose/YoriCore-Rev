package Core.Device

import chisel3._
import chisel3.util._

import Core.Bus._
import Core.Config.Config

class DeviceController(val regions: Seq[DeviceRegion], val dataWidth: Int = Config.Bus.AxiDataWidth) extends Module {
  require(regions.nonEmpty)

  val io = IO(new Bundle {
    val axi     = Flipped(new AXI4Master(dataWidth))
    val devices = Vec(regions.size, new DeviceBus(dataWidth))
  })

  val sIdle :: sWReq :: sWWait :: sWResp :: sWErr :: sRReq :: sRWait :: sRResp :: sRErr :: Nil = Enum(9)
  val state                                                                             = RegInit(sIdle)

  val awValid = RegInit(false.B)
  val awAddr  = RegInit(0.U(AXI4.AddrWidth.W))
  val awId    = RegInit(0.U(AXI4.IdWidth.W))
  val awSize  = RegInit(0.U(3.W))
  val awBad   = RegInit(false.B)

  val wValid = RegInit(false.B)
  val wData  = RegInit(0.U(dataWidth.W))
  val wStrb  = RegInit(0.U((dataWidth / 8).W))

  val arValid = RegInit(false.B)
  val arAddr  = RegInit(0.U(AXI4.AddrWidth.W))
  val arId    = RegInit(0.U(AXI4.IdWidth.W))
  val arSize  = RegInit(0.U(3.W))
  val arBad   = RegInit(false.B)

  val respData = RegInit(0.U(dataWidth.W))
  val respErr  = RegInit(false.B)

  val writeAddr    = Mux(awValid, awAddr, arAddr)
  val writeRoute   = VecInit(regions.map(_.contains(writeAddr)))
  val writeRouteOk = writeRoute.asUInt.orR
  val writeSel     = PriorityEncoder(writeRoute)

  val readRoute   = VecInit(regions.map(_.contains(arAddr)))
  val readRouteOk = readRoute.asUInt.orR
  val readSel     = PriorityEncoder(readRoute)

  io.axi.aw.ready := state === sIdle && !awValid
  io.axi.w.ready  := state === sIdle && !wValid
  io.axi.ar.ready := state === sIdle && !awValid && !wValid && !arValid && !io.axi.aw.valid

  when(state === sIdle) {
    when(io.axi.aw.fire) {
      awValid := true.B
      awAddr  := io.axi.aw.bits.addr
      awId    := io.axi.aw.bits.id
      awSize  := io.axi.aw.bits.size
      awBad   := io.axi.aw.bits.len =/= 0.U || io.axi.aw.bits.burst =/= AXI4.Burst.INCR || io.axi.aw.bits.size > 3.U
    }
    when(io.axi.w.fire) {
      wValid := true.B
      wData  := io.axi.w.bits.data
      wStrb  := io.axi.w.bits.strb
    }
    when(io.axi.ar.fire) {
      arValid := true.B
      arAddr  := io.axi.ar.bits.addr
      arId    := io.axi.ar.bits.id
      arSize  := io.axi.ar.bits.size
      arBad   := io.axi.ar.bits.len =/= 0.U || io.axi.ar.bits.burst =/= AXI4.Burst.INCR || io.axi.ar.bits.size > 3.U
      state   := sRReq
    }.elsewhen(awValid && wValid) {
      state := sWReq
    }
  }

  for (i <- regions.indices) {
    io.devices(i).req.valid     := false.B
    io.devices(i).req.bits.addr  := 0.U
    io.devices(i).req.bits.write := false.B
    io.devices(i).req.bits.size  := 0.U
    io.devices(i).req.bits.wdata := 0.U
    io.devices(i).req.bits.wstrb := 0.U
    io.devices(i).resp.ready     := false.B
  }

  when(state === sWReq) {
    when(!awBad && writeRouteOk) {
      io.devices(writeSel).req.valid      := true.B
      io.devices(writeSel).req.bits.addr  := awAddr
      io.devices(writeSel).req.bits.write := true.B
      io.devices(writeSel).req.bits.size  := awSize
      io.devices(writeSel).req.bits.wdata := wData
      io.devices(writeSel).req.bits.wstrb := wStrb
      when(io.devices(writeSel).req.ready) { state := sWWait }
    }.otherwise {
      respErr := true.B
      state   := sWErr
    }
  }

  when(state === sWWait) {
    io.devices(writeSel).resp.ready := true.B
    when(io.devices(writeSel).resp.valid) {
      respErr := io.devices(writeSel).resp.bits.err
      state   := sWResp
    }
  }

  io.axi.b.valid     := state === sWResp || state === sWErr
  io.axi.b.bits.id   := awId
  io.axi.b.bits.resp := Mux(state === sWErr || respErr, AXI4.Resp.SLVERR, AXI4.Resp.OKAY)

  when(io.axi.b.fire) {
    awValid := false.B
    wValid  := false.B
    respErr := false.B
    state   := sIdle
  }

  when(state === sRReq) {
    when(!arBad && readRouteOk) {
      io.devices(readSel).req.valid      := true.B
      io.devices(readSel).req.bits.addr  := arAddr
      io.devices(readSel).req.bits.write := false.B
      io.devices(readSel).req.bits.size  := arSize
      io.devices(readSel).req.bits.wdata := 0.U
      io.devices(readSel).req.bits.wstrb := 0.U
      when(io.devices(readSel).req.ready) { state := sRWait }
    }.otherwise {
      respData := 0.U
      respErr  := true.B
      state    := sRErr
    }
  }

  when(state === sRWait) {
    io.devices(readSel).resp.ready := true.B
    when(io.devices(readSel).resp.valid) {
      respData := io.devices(readSel).resp.bits.rdata
      respErr  := io.devices(readSel).resp.bits.err
      state    := sRResp
    }
  }

  io.axi.r.valid     := state === sRResp || state === sRErr
  io.axi.r.bits.id   := arId
  io.axi.r.bits.data := respData
  io.axi.r.bits.resp := Mux(state === sRErr || respErr, AXI4.Resp.SLVERR, AXI4.Resp.OKAY)
  io.axi.r.bits.last := true.B

  when(io.axi.r.fire) {
    arValid  := false.B
    respData := 0.U
    respErr  := false.B
    state    := sIdle
  }
}