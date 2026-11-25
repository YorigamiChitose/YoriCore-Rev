package Core.Cache

import chisel3._
import chisel3.util._
import Core.Config.Config
import Core.Bus._

// 一次访存的解析结果（命中与未命中路径共用）
class MemResolve extends Bundle {
  val line     = UInt(Config.Cache.LineBits.W) // 写回后的行
  val dirty    = Bool()                        // 是否置脏
  val resp     = UInt(Config.Data.XLEN.W)      // 返回给 CPU 的字（右对齐）
  val resSet   = Bool()                        // 置 reservation（LR）
  val resClear = Bool()                        // 清 reservation（SC）
  val resAddr  = UInt(Config.Addr.Width.W)     // LR 时记录地址
}

// 组相联、写回、写分配的 L1 缓存，树形 PLRU 替换。
// 未命中：先写回脏行（若需），再从存储器读整行填入后，再返回给 CPU。
// 可缓存地址（落在 Mem 区间内）进缓存，其余（MMIO 及其它）直通到 io.mem 绕过缓存。
// 唯一存储器侧接口 io.mem（统一 MemPort），行访问 size=Line，字访问 size=B/H/W。
class L1Cache extends Module {
  val io = IO(new Bundle {
    val cpu       = Flipped(new MemPort) // CPU 侧（字访问，从侧）
    val mem       = new MemPort          // 存储器侧（统一请求，主侧）
    val fence     = Input(Bool())        // fence.i：回写脏行并全清
    val hold      = Input(Bool())        // 排空完成后仍暂停对外服务（等待排序）
    val fenceDone = Output(Bool())       // 排空完成脉冲
  })

  val obsHit  = if (Config.Sim.Enable) Some(IO(Output(Bool()))) else None
  val obsMiss = if (Config.Sim.Enable) Some(IO(Output(Bool()))) else None

  val lineBits = Config.Cache.LineBits
  val xlen     = Config.Data.XLEN

  // 存储阵列：tag / data / valid / dirty(每组) + plru(每组)
  val tag   = RegInit(VecInit(Seq.fill(Config.Cache.NumWays)(VecInit(Seq.fill(Config.Cache.NumSets)(0.U(Config.Cache.TagWidth.W))))))
  val data  = RegInit(VecInit(Seq.fill(Config.Cache.NumWays)(VecInit(Seq.fill(Config.Cache.NumSets)(0.U(lineBits.W))))))
  val valid = RegInit(VecInit(Seq.fill(Config.Cache.NumWays)(VecInit(Seq.fill(Config.Cache.NumSets)(false.B)))))
  val dirty = RegInit(VecInit(Seq.fill(Config.Cache.NumWays)(VecInit(Seq.fill(Config.Cache.NumSets)(false.B)))))
  val plru  = RegInit(VecInit(Seq.fill(Config.Cache.NumSets)(0.U(Config.Cache.PLRUBits.W))))

  // 请求分解（字部分取低 32 位）
  val reqAddr     = io.cpu.req.bits.addr
  // PMA 完备检查：内存区间与设备区间都显式判断，cache（可缓存）与 mem（直通）共用。
  // 可缓存 = 落在内存区间内 且 不属于任何 MMIO 设备区间（不靠排除法省略设备检查）。
  val inMem       = Config.Pma.isMem(reqAddr)
  val inMmio      = Config.Pma.isMmio(reqAddr)
  val cacheable   = inMem && !inMmio
  val reqWrite    = io.cpu.req.bits.write
  val reqData     = io.cpu.req.bits.wdata(xlen - 1, 0)
  val reqWstrb    = io.cpu.req.bits.wstrb((xlen / 8) - 1, 0)
  val reqSize     = io.cpu.req.bits.size
  val reqAmo      = io.cpu.req.bits.amoOp
  val byteOffWord = reqAddr(1, 0)
  val reqAlign    = Cat(reqAddr(Config.Addr.Width - 1, Config.Cache.LineWidth), 0.U(Config.Cache.LineWidth.W))
  val index       = reqAddr(Config.Cache.IndexWidth + Config.Cache.LineWidth - 1, Config.Cache.LineWidth)
  val reqTag      = reqAddr(Config.Addr.Width - 1, Config.Cache.IndexWidth + Config.Cache.LineWidth)
  val wordIdx     = reqAddr(Config.Cache.LineWidth - 1, 2)
  val byteOffLine = reqAddr(Config.Cache.LineWidth - 1, 0)

  // 命中检测
  val hitVec = VecInit((0 until Config.Cache.NumWays).map(w => valid(w)(index) && tag(w)(index) === reqTag))
  val hit    = hitVec.asUInt.orR
  val hitWay = PriorityEncoder(hitVec.asUInt)

  // PLRU 替换候选（树形）
  val victim = Mux(plru(index)(Config.Cache.PLRUBits - 1), Mux(plru(index)(Config.Cache.PLRUBits - 2), 0.U, 1.U), Mux(plru(index)(Config.Cache.PLRUBits - 3), 2.U, 3.U))

  // 行内读：按 size 取出字节/半字/字并右对齐（符号扩展归消费者）
  def readLine(line: UInt, widx: UInt, byteOff: UInt, size: UInt): UInt = {
    val word    = (line >> (widx * xlen.U))(xlen - 1, 0)
    val byteSel = (word >> (byteOff * 8.U))(7, 0)
    val halfSel = (word >> (byteOff(1) * 16.U))(15, 0)
    Mux(size === 1.U, Cat(Fill(16, 0.U), halfSel), Mux(size === 0.U, Cat(Fill(24, 0.U), byteSel), word))
  }

  // 行内写：按 wstrb 写入字节/半字/字（wdata/wstrb 右对齐，按 byteOff 摆放）
  def writeLine(line: UInt, byteOff: UInt, wd: UInt, strb: UInt): UInt = {
    val strb32 = Cat(Fill(8, strb(3)), Fill(8, strb(2)), Fill(8, strb(1)), Fill(8, strb(0)))
    val mask   = Cat(0.U((lineBits - xlen).W), strb32) << (byteOff * 8.U)
    val dval   = Cat(0.U((lineBits - xlen).W), wd) << (byteOff * 8.U)
    (line & ~mask) | (dval & mask)
  }

  // AMO 新值（AmoOp.ADD 为默认）
  def atomicNew(oldW: UInt, op: UInt, d: UInt): UInt = MuxCase(
    oldW + d,
    Seq(
      (op === AmoOp.SWAP) -> d,
      (op === AmoOp.XOR)  -> (oldW ^ d),
      (op === AmoOp.AND)  -> (oldW & d),
      (op === AmoOp.OR)   -> (oldW | d),
      (op === AmoOp.MIN)  -> Mux(oldW.asSInt < d.asSInt, oldW, d),
      (op === AmoOp.MAX)  -> Mux(oldW.asSInt > d.asSInt, oldW, d),
      (op === AmoOp.MINU) -> Mux(oldW < d, oldW, d),
      (op === AmoOp.MAXU) -> Mux(oldW > d, oldW, d)
    )
  )

  // 统一访存解析（命中/未命中共用）：普通读/写、AMO/LR/SC 都收敛在这一处
  def resolve(
    line:  UInt,
    addr:  UInt,
    widx:  UInt,
    boW:   UInt,
    boL:   UInt,
    write: Bool,
    size:  UInt,
    d:     UInt,
    strb:  UInt,
    amo:   UInt,
    scOk:  Bool
  ): MemResolve = {
    val r       = Wire(new MemResolve)
    val isAmo   = amo =/= AmoOp.NOP
    val isLr    = amo === AmoOp.LR
    val isSc    = amo === AmoOp.SC
    val oldW    = readLine(line, widx, boW, MemSize.W)
    val wrLine  = writeLine(line, boL, d, strb)
    val amoLine = writeLine(line, boL, atomicNew(oldW, amo, d), strb)

    r.line     := MuxCase(
      line,
      Seq(
        (write && !isAmo)         -> wrLine, // 普通写
        (isSc && scOk)            -> wrLine, // SC 成功
        (isAmo && !isLr && !isSc) -> amoLine // AMO
      )
    )
    r.dirty    := (write && !isAmo) || (isSc && scOk) || (isAmo && !isLr && !isSc)
    r.resp     := Mux(isSc, Mux(scOk, 0.U(xlen.W), 1.U(xlen.W)), Mux(isAmo, oldW, readLine(line, widx, boW, size)))
    r.resSet   := isLr
    r.resClear := isSc
    r.resAddr  := addr
    r
  }

  // 树形 PLRU 更新（把 way 标记为 MRU），命中/填充共用
  def updatePLRU(set: UInt, way: UInt): Unit = {
    val op  = plru(set)
    val nb1 = Mux(way(1) === 0.U, way(0), op(Config.Cache.PLRUBits - 2))
    val nb0 = Mux(way(1) === 1.U, way(0), op(Config.Cache.PLRUBits - 3))
    plru(set) := Cat(way(1), nb1, nb0)
  }

  // 状态机
  val sIdle :: sEvict :: sEvictWait :: sFetch :: sFetchWait :: sResp :: Nil = Enum(6)
  val state                                                                 = RegInit(sIdle)

  val rAddr    = RegInit(0.U(Config.Addr.Width.W))
  val rAlign   = RegInit(0.U(Config.Addr.Width.W))
  val rIndex   = RegInit(0.U(Config.Cache.IndexWidth.W))
  val rTag     = RegInit(0.U(Config.Cache.TagWidth.W))
  val rWord    = RegInit(0.U(Config.Cache.WordBits.W))
  val rWrite   = RegInit(false.B)
  val rData    = RegInit(0.U(xlen.W))
  val rSize    = RegInit(0.U(2.W))
  val rWstrb   = RegInit(0.U((xlen / 8).W))
  val rByteOff = RegInit(0.U(Config.Cache.LineWidth.W))
  val rWay     = RegInit(0.U(log2Ceil(Config.Cache.NumWays).W))
  val rAmo     = RegInit(0.U(4.W))
  val resValid = RegInit(false.B)
  val resAddr  = RegInit(0.U(Config.Addr.Width.W))

  val respWord = RegInit(0.U(xlen.W))

  // fence.i 排空
  val draining = RegInit(false.B)
  val drainCnt = RegInit(0.U((log2Ceil(Config.Cache.NumSets * Config.Cache.NumWays) + 1).W))
  val drainWb  = RegInit(false.B)

  val uncached = !cacheable
  io.cpu.req.ready := Mux(uncached, io.mem.req.ready, (state === sIdle) && !draining && !io.hold)
  val reqFire   = io.cpu.req.valid && io.cpu.req.ready && cacheable
  val needEvict = !hit && dirty(victim)(index)

  obsHit.foreach(_ := reqFire && hit)
  obsMiss.foreach(_ := reqFire && !hit)

  // PLRU 更新（命中路径）
  when(state === sIdle && reqFire && hit) {
    updatePLRU(index, hitWay)
  }

  // —— FSM 行请求（由 switch / drain 驱动）——
  val fsmReqValid  = WireDefault(false.B)
  val fsmReqAddr   = WireDefault(0.U(Config.Addr.Width.W))
  val fsmReqWrite  = WireDefault(false.B)
  val fsmReqWdata  = WireDefault(0.U(lineBits.W))
  val fsmRespReady = WireDefault(false.B)

  switch(state) {
    is(sIdle) {
      when(reqFire) {
        rAddr    := reqAddr
        rAlign   := reqAlign
        rIndex   := index
        rTag     := reqTag
        rWord    := wordIdx
        rWrite   := reqWrite
        rData    := reqData
        rSize    := reqSize
        rWstrb   := reqWstrb
        rByteOff := byteOffLine
        rAmo     := reqAmo
        when(!hit) { rWay := victim }
        when(!hit) {
          state := Mux(needEvict, sEvict, sFetch)
        }.otherwise {
          val m = resolve(data(hitWay)(index), reqAddr, wordIdx, byteOffWord, byteOffLine, reqWrite, reqSize, reqData, reqWstrb, reqAmo, resValid && (resAddr === reqAddr))
          data(hitWay)(index) := m.line
          when(m.dirty) { dirty(hitWay)(index) := true.B }
          when(m.resSet) { resValid := true.B; resAddr := m.resAddr }
          when(m.resClear) { resValid := false.B }
          respWord            := m.resp
          state               := sResp
        }
      }
    }
    is(sEvict) {
      fsmReqValid         := true.B
      fsmReqAddr          := Cat(tag(rWay)(rIndex), rIndex, 0.U(Config.Cache.LineWidth.W))
      fsmReqWrite         := true.B
      fsmReqWdata         := data(rWay)(rIndex)
      dirty(rWay)(rIndex) := false.B
      when(io.mem.req.ready) { state := sEvictWait }
    }
    is(sEvictWait) {
      fsmRespReady := true.B
      when(io.mem.resp.valid) { state := sFetch }
    }
    is(sFetch) {
      fsmReqValid := true.B
      fsmReqAddr  := rAlign
      fsmReqWrite := false.B
      when(io.mem.req.ready) { state := sFetchWait }
    }
    is(sFetchWait) {
      fsmRespReady := true.B
      when(io.mem.resp.valid) {
        val baseLine = io.mem.resp.bits.rdata
        val m        = resolve(baseLine, rAddr, rWord, rByteOff(1, 0), rByteOff, rWrite, rSize, rData, rWstrb, rAmo, resValid && (resAddr === rAddr))
        data(rWay)(rIndex)  := m.line
        tag(rWay)(rIndex)   := rTag
        valid(rWay)(rIndex) := true.B
        when(m.dirty) { dirty(rWay)(rIndex) := true.B }
        when(m.resSet) { resValid := true.B; resAddr := m.resAddr }
        when(m.resClear) { resValid := false.B }
        updatePLRU(rIndex, rWay)
        respWord            := m.resp
        state               := sResp
      }
    }
    is(sResp) {
      when(io.cpu.resp.ready) { state := sIdle }
    }
  }

  // —— 统一存储侧请求：不可缓存直通 vs 行请求 ——
  io.mem.req.valid      := Mux(uncached, io.cpu.req.valid, fsmReqValid)
  io.mem.req.bits.addr  := Mux(uncached, io.cpu.req.bits.addr, fsmReqAddr)
  io.mem.req.bits.write := Mux(uncached, io.cpu.req.bits.write, fsmReqWrite)
  io.mem.req.bits.size  := Mux(uncached, io.cpu.req.bits.size, MemSize.Line)
  io.mem.req.bits.wdata := Mux(uncached, io.cpu.req.bits.wdata, fsmReqWdata)
  io.mem.req.bits.wstrb := Mux(uncached, io.cpu.req.bits.wstrb, Fill(lineBits / 8, 1.U))
  io.mem.req.bits.amoOp := Mux(uncached, io.cpu.req.bits.amoOp, AmoOp.NOP)

  io.mem.resp.ready := Mux(uncached, io.cpu.resp.ready, fsmRespReady)

  io.cpu.resp.valid      := Mux(uncached, io.mem.resp.valid, state === sResp)
  io.cpu.resp.bits.rdata := Mux(uncached, io.mem.resp.bits.rdata, Cat(0.U((lineBits - xlen).W), respWord))
  io.cpu.resp.bits.err   := Mux(uncached, io.mem.resp.bits.err, false.B)

  // ---- fence.i 排空（回写所有脏行 + 全清）----
  val curWay   = (drainCnt % Config.Cache.NumWays.U)(log2Ceil(Config.Cache.NumWays) - 1, 0)
  val curIdx   = (drainCnt / Config.Cache.NumWays.U)(Config.Cache.IndexWidth - 1, 0)
  val curDirty = dirty(curWay)(curIdx)
  val total    = Config.Cache.NumSets * Config.Cache.NumWays

  io.fenceDone := draining && (drainCnt === total.U)

  when(!draining && io.fence && state === sIdle) {
    draining := true.B
    drainCnt := 0.U
    drainWb  := false.B
  }

  when(draining) {
    when(drainCnt === total.U) {
      draining := false.B
    }.elsewhen(drainWb) {
      fsmRespReady := true.B
      when(io.mem.resp.valid) {
        drainWb  := false.B
        drainCnt := drainCnt + 1.U
      }
    }.elsewhen(curDirty) {
      fsmReqValid           := true.B
      fsmReqAddr            := Cat(tag(curWay)(curIdx), curIdx, 0.U(Config.Cache.LineWidth.W))
      fsmReqWrite           := true.B
      fsmReqWdata           := data(curWay)(curIdx)
      dirty(curWay)(curIdx) := false.B
      valid(curWay)(curIdx) := false.B
      when(io.mem.req.ready) { drainWb := true.B }
    }.otherwise {
      valid(curWay)(curIdx) := false.B
      drainCnt              := drainCnt + 1.U
    }
  }
}
