package Core.IDU.module

import chisel3._
import chisel3.util._

// RV32C 压缩指令展开器：16 位指令 -> 32 位规范指令。
// 输出与现有 RV32I/RV32M 译码表的 BitPat 完全对齐，IDU 无需为 C 单独建表。
object CExpander {
  def expand(i: UInt): UInt = {
    // 32 位基础 opcode（低位 7 位）
    val OP_IMM = "b0010011".U(7.W) // 立即数运算
    val OP     = "b0110011".U(7.W) // 寄存器运算
    val LOAD   = "b0000011".U(7.W) // 装载
    val STORE  = "b0100011".U(7.W) // 存储
    val LUI    = "b0110111".U(7.W)
    val JAL    = "b1101111".U(7.W)
    val JALR   = "b1100111".U(7.W)
    val BR     = "b1100011".U(7.W)
    val SYS    = "b1110011".U(7.W)

    val X0 = "b00000".U(5.W)
    val X1 = "b00001".U(5.W)
    val X2 = "b00010".U(5.W)

    // 3 位 funct3
    val F_ADD  = "b000".U(3.W)
    val F_LW   = "b010".U(3.W)
    val F_SW   = "b010".U(3.W)
    val F_SR   = "b101".U(3.W) // SRLI/SRAI
    val F_ANDI = "b111".U(3.W)
    val F_SLL  = "b001".U(3.W) // SLLI
    val F_XOR  = "b100".U(3.W)
    val F_OR   = "b110".U(3.W)
    val F_AND  = "b111".U(3.W)
    val F_BEQ  = "b000".U(3.W)
    val F_BNE  = "b001".U(3.W)
    val F_SUB  = "b000".U(3.W)

    // 7 位 funct7
    val F7_0  = "b0000000".U(7.W)
    val F7_20 = "b0100000".U(7.W)

    // 压缩寄存器：8 + x   (x 为 3 位)
    def rP(x: UInt): UInt = Cat("b01".U(2.W), x)

    // 构造器（按标准 32 位位场）
    def rType(f7: UInt, rs2: UInt, rs1: UInt, f3: UInt, rd: UInt, op: UInt): UInt =
      Cat(f7, rs2, rs1, f3, rd, op)
    def iType(imm: UInt, rs1: UInt, f3: UInt, rd: UInt, op: UInt):           UInt =
      Cat(imm, rs1, f3, rd, op) // imm 12 位 [31:20]
    def sType(immH: UInt, rs2: UInt, rs1: UInt, f3: UInt, immL: UInt, op: UInt): UInt =
      Cat(immH, rs2, rs1, f3, immL, op) // immH 7 位 [11:5], immL 5 位 [4:0]
    def bType(imm12: UInt, imm10_5: UInt, rs2: UInt, rs1: UInt, f3: UInt, imm4_1: UInt, imm11: UInt, op: UInt): UInt =
      Cat(imm12, imm10_5, rs2, rs1, f3, imm4_1, imm11, op)
    def uType(imm: UInt, rd: UInt, op: UInt):                                                                   UInt =
      Cat(imm, rd, op) // imm 20 位 [31:12]
    def jType(imm20: UInt, imm10_1: UInt, imm11: UInt, imm19_12: UInt, rd: UInt, op: UInt): UInt =
      Cat(imm20, imm10_1, imm11, imm19_12, rd, op)

    // 保留/非法：opcode=0001011，不在译码表内 -> ILLEGAL_INST
    val ILLEGAL = Cat(0.U(25.W), "b0001011".U(7.W))

    val f3  = i(15, 13)
    val b12 = i(12)
    val r5  = i(11, 7) // CI/CR 里的 5 位 rd/rs1
    val r3a = i(9, 7)  // 压缩 rs1'/rd' (3 位)
    val r5b = i(6, 2)  // 5 位 imm / rs2 / shamt 低位
    val r3b = i(4, 2)  // 压缩 rd'/rs2' (3 位)
    val op  = i(1, 0)

    val rdA = rP(r3b) // 8 + [4:2]
    val rsA = rP(r3a) // 8 + [9:7]

    // 常用立即数
    val imm6   = Cat(b12, r5b) // CI: {nzimm[5], nzimm[4:0]}
    val shamt6 = Cat(b12, r5b)

    // ---- Quadrant 0 (op == 00)，压缩寄存器 8..15 ----

    // C.ADDI4SPN: f3=000, nzuimm[9:2] = {[10:7],[12:11],[5],[6]}
    val nzA      = Cat(i(10, 7), i(12, 11), i(5), i(6)) // nzuimm[9:2]
    val addi4spn = iType(Cat(0.U(2.W), nzA, 0.U(2.W)), X2, F_ADD, rdA, OP_IMM)

    // C.LW: f3=010, uimm[6:2] = {[5],[12:10],[6]}
    val uimmCL = Cat(i(5), i(12, 10), i(6)) // uimm[6:2]
    val c_lw   = iType(Cat(0.U(5.W), uimmCL, 0.U(2.W)), rsA, F_LW, rdA, LOAD)

    // C.SW: f3=110
    val sw_immH = Cat(0.U(5.W), uimmCL(4, 3))
    val sw_immL = Cat(uimmCL(2, 0), 0.U(2.W))
    val c_sw    = sType(sw_immH, rdA, rsA, F_SW, sw_immL, STORE)

    // ---- Quadrant 1 (op == 01) ----

    // C.ADDI / C.NOP: f3=000
    val c_addi = iType(Cat(Fill(6, b12), imm6), r5, F_ADD, r5, OP_IMM)

    // C.JAL: f3=001 (RV32), offset[11:0] 重组
    val jalSign = b12
    val jal10_1 = Cat(i(8), i(10), i(9), i(6), i(7), i(2), i(11), i(5), i(4), i(3))
    val c_jal   = jType(jalSign, jal10_1, jalSign, Fill(8, jalSign), X1, JAL)

    // C.LI: f3=010
    val c_li = iType(Cat(Fill(6, b12), imm6), X0, F_ADD, r5, OP_IMM)

    // C.ADDI16SP / C.LUI: f3=011
    val nzAddi16     = Cat(b12, i(4, 3), i(5), i(2), i(6))
    val c_addi16sp   = iType(Cat(Fill(2, b12), nzAddi16, 0.U(4.W)), X2, F_ADD, X2, OP_IMM)
    val lui_imm17_12 = Cat(b12, r5b)
    val c_lui        = uType(Cat(Fill(14, b12), lui_imm17_12), r5, LUI)

    // C.SRLI/C.SRAI/C.ANDI (f3=100, [11:10]=00/01/10), 压缩寄存器
    val c_srli = iType(Cat("b000000".U(6.W), shamt6), rsA, F_SR, rsA, OP_IMM)
    val c_srai = iType(Cat("b010000".U(6.W), shamt6), rsA, F_SR, rsA, OP_IMM)
    val c_andi = iType(Cat(Fill(6, b12), imm6), rsA, F_ANDI, rsA, OP_IMM)

    // C.SUB/XOR/OR/AND (f3=100, [12:10]=011, [6:5]=funct2), 压缩寄存器
    val c_sub = rType(F7_20, rdA, rsA, F_SUB, rsA, OP)
    val c_xor = rType(F7_0, rdA, rsA, F_XOR, rsA, OP)
    val c_or  = rType(F7_0, rdA, rsA, F_OR, rsA, OP)
    val c_and = rType(F7_0, rdA, rsA, F_AND, rsA, OP)

    // C.J: f3=101, offset 重组 (jal x0)
    val c_j = jType(jalSign, jal10_1, jalSign, Fill(8, jalSign), X0, JAL)

    // C.BEQZ/C.BNEZ: f3=110/111, offset[8:0] = {[12],[6],[5],[2],[11],[10],[4],[3],0}
    val bd12   = b12
    val bd10_5 = Cat(b12, b12, b12, i(6), i(5), i(2))
    val bd4_1  = Cat(i(11), i(10), i(4), i(3))
    val c_beqz = bType(bd12, bd10_5, X0, rsA, F_BEQ, bd4_1, b12, BR)
    val c_bnez = bType(bd12, bd10_5, X0, rsA, F_BNE, bd4_1, b12, BR)

    // ---- Quadrant 2 (op == 10) ----

    // C.SLLI: f3=000, rd 为 5 位
    val c_slli = iType(Cat("b000000".U(6.W), shamt6), r5, F_SLL, r5, OP_IMM)

    // C.LWSP: f3=010, uimm[7:6]=[3:2], uimm[5]=[12], uimm[4:2]=[6:4]
    val uimmLWSP = Cat(i(3, 2), i(12), i(6, 4)) // uimm[7:2]
    val c_lwsp   = iType(Cat(0.U(4.W), uimmLWSP, 0.U(2.W)), X2, F_LW, r5, LOAD)

    // C.SWSP: f3=110, uimm[7:6]=[8:7], uimm[5:2]=[12:9], rs2=[6:2]
    val uimmSWSP = Cat(i(8, 7), i(12, 9)) // uimm[7:2]
    val csw_immH = Cat(0.U(4.W), uimmSWSP(5, 3))
    val csw_immL = Cat(uimmSWSP(2, 0), 0.U(2.W))
    val c_swsp   = sType(csw_immH, r5b, X2, F_SW, csw_immL, STORE)

    // CR: funct4=[15:12], rs1/rd=[11:7], rs2=[6:2]
    val f4       = i(15, 12)
    val c_jr     = iType(0.U(12.W), r5, F_ADD, X0, JALR)             // jalr x0, 0(rs1)
    val c_mv     = rType(F7_0, r5b, X0, F_ADD, r5, OP)               // add rd, x0, rs2
    val c_ebreak = iType("h001".U(12.W), X0, "b000".U(3.W), X0, SYS) // ebreak
    val c_jalr   = iType(0.U(12.W), r5, F_ADD, X1, JALR)             // jalr x1, 0(rs1)
    val c_add    = rType(F7_0, r5b, r5, F_ADD, r5, OP)               // add rd, rd, rs2

    // 综合选择
    val res = MuxCase(
      ILLEGAL,
      Seq(
        // Quadrant 0
        (op === "b00".U && f3 === "b000".U)                                                  -> addi4spn,
        (op === "b00".U && f3 === "b010".U)                                                  -> c_lw,
        (op === "b00".U && f3 === "b110".U)                                                  -> c_sw,
        // Quadrant 1
        (op === "b01".U && f3 === "b000".U)                                                  -> c_addi,
        (op === "b01".U && f3 === "b001".U)                                                  -> c_jal,
        (op === "b01".U && f3 === "b010".U)                                                  -> c_li,
        (op === "b01".U && f3 === "b011".U && r5 === 2.U)                                    -> c_addi16sp,
        (op === "b01".U && f3 === "b011".U && r5 =/= 2.U)                                    -> c_lui,
        (op === "b01".U && f3 === "b100".U && i(11, 10) === "b00".U)                         -> c_srli,
        (op === "b01".U && f3 === "b100".U && i(11, 10) === "b01".U)                         -> c_srai,
        (op === "b01".U && f3 === "b100".U && i(11, 10) === "b10".U)                         -> c_andi,
        (op === "b01".U && f3 === "b100".U && i(12, 10) === "b011".U && i(6, 5) === "b00".U) -> c_sub,
        (op === "b01".U && f3 === "b100".U && i(12, 10) === "b011".U && i(6, 5) === "b01".U) -> c_xor,
        (op === "b01".U && f3 === "b100".U && i(12, 10) === "b011".U && i(6, 5) === "b10".U) -> c_or,
        (op === "b01".U && f3 === "b100".U && i(12, 10) === "b011".U && i(6, 5) === "b11".U) -> c_and,
        (op === "b01".U && f3 === "b101".U)                                                  -> c_j,
        (op === "b01".U && f3 === "b110".U)                                                  -> c_beqz,
        (op === "b01".U && f3 === "b111".U)                                                  -> c_bnez,
        // Quadrant 2
        (op === "b10".U && f3 === "b000".U)                                                  -> c_slli,
        (op === "b10".U && f3 === "b010".U)                                                  -> c_lwsp,
        (op === "b10".U && f3 === "b110".U)                                                  -> c_swsp,
        // CR: funct4
        (op === "b10".U && f4 === "b1000".U && r5b =/= 0.U)                                  -> c_mv,
        (op === "b10".U && f4 === "b1000".U && r5b === 0.U)                                  -> c_jr,
        (op === "b10".U && f4 === "b1001".U && r5 === 0.U && r5b === 0.U)                    -> c_ebreak,
        (op === "b10".U && f4 === "b1001".U && r5 =/= 0.U && r5b === 0.U)                    -> c_jalr,
        (op === "b10".U && f4 === "b1001".U && r5 =/= 0.U && r5b =/= 0.U)                    -> c_add
      )
    )

    res
  }
}
