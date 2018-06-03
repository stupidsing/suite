package suite.assembler;

import static suite.util.Friends.min;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpImm;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.funp.Funp_;
import suite.os.LogUtil;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.primitive.IntObj_Int;
import suite.primitive.IntPrimitives.IntSink;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.adt.map.IntIntMap;
import suite.util.Fail;
import suite.util.FunUtil.Sink;
import suite.util.To;

public class Amd64Interpret {

	public final BytesBuilder out = new BytesBuilder();

	private int base0 = 0;
	private int baseCode0 = base0 + 0;
	private int baseCodex = baseCode0 + 0x10000000;
	private int baseStack0 = baseCodex + 0;
	private int baseStackx = baseStack0 + 0x40000;
	// private int basex = baseStackx + 0;

	private int position0 = 0;
	private int positionCode0 = position0 + 0;
	private int positionCodex = position0 + 65536;
	private int positionStack0 = positionCodex + 0;
	private int positionStackx = positionStack0 + 262144;
	private int positionx = positionStackx + 0;

	private int diffCode = baseCode0 - positionCode0;
	private int diffStack = baseStackx - positionStackx;
	private ByteBuffer mem = ByteBuffer.allocate(positionx);
	private int[] regs = new int[16];
	private int c;

	private Amd64 amd64 = Amd64.me;
	private Amd64Dump dump = new Amd64Dump();
	private int eax = amd64.eax.reg;
	private int ebx = amd64.ebx.reg;
	private int ecx = amd64.ecx.reg;
	private int edx = amd64.edx.reg;
	private int esp = amd64.esp.reg;
	private int esi = amd64.esi.reg;
	private int edi = amd64.edi.reg;
	private int eip;

	private int[] scales = { 1, 2, 4, 8, };

	private Sink<Bytes> output = out::append;

	public Amd64Interpret() {
	}

	public int interpret(List<Instruction> instructions, Bytes code, Bytes input) {
		mem.order(ByteOrder.LITTLE_ENDIAN);
		mem.position(positionCode0);
		mem.put(code.bs);
		eip = positionCode0;
		regs[esp] = baseStackx - 16;

		var labels = new IntIntMap();

		for (var i = 0; i < instructions.size(); i++) {
			var i_ = i;
			var instruction = instructions.get(i_);
			if (instruction.insn == Insn.LABEL)
				labels.update((int) ((OpImm) instruction.op0).imm, i0 -> i_ + 1);
		}

		while (true) {
			var instruction = instructions.get(eip++);

			if (Boolean.FALSE)
				LogUtil.info(state(instruction));

			try {
				IntObj_Int<Operand> trim = (i, op) -> {
					if (op.size == 1)
						return (int) (byte) i;
					else
						return i;
				};

				Obj_Int<Operand> fetch32 = op -> {
					int v0;
					if (op instanceof OpImm)
						v0 = (int) ((OpImm) op).imm;
					else if (op instanceof OpMem)
						v0 = mem.getInt(index(address((OpMem) op)));
					else if (op instanceof OpReg) {
						var reg = ((OpReg) op).reg;
						v0 = regs[reg];
					} else
						v0 = 0;
					return trim.apply(v0, op);
				};

				var op0 = instruction.op0;
				var op1 = instruction.op1;
				int source0 = fetch32.apply(op0);
				int source1 = fetch32.apply(op1);
				IntSink assign;

				if (op0 instanceof OpMem) {
					var index = index(address((OpMem) op0));
					if (op0.size == 1)
						assign = i -> mem.put(index, (byte) i);
					else if (op0.size == 4)
						assign = i -> mem.putInt(index, i);
					else
						assign = null;
				} else if (op0 instanceof OpReg) {
					var reg = ((OpReg) op0).reg;
					if (op0.size == 1)
						assign = i -> regs[reg] = regs[reg] & 0xFFFFFF00 | i;
					else if (op0.size == 4)
						assign = i -> regs[reg] = i;
					else
						assign = null;
				} else
					assign = null;

				switch (instruction.insn) {
				case ADD:
					assign.sink(source0 + source1);
					break;
				case AND:
					assign.sink(source0 & source1);
					break;
				case CALL:
					push(eip);
					eip = labels.get(source0);
					break;
				case CLD:
					break;
				case CMP:
					c = Integer.compare(source0, source1);
					break;
				case DEC:
					assign.sink(source0 - 1);
					break;
				case INC:
					assign.sink(source0 + 1);
					break;
				case INT:
					if ((byte) source0 == -128)
						if (regs[eax] == 1) // exit
							return regs[ebx];
						else if (regs[eax] == 3) { // read
							int length = min(regs[edx], input.size());
							var di = index(regs[ecx]);
							for (var i = 0; i < length; i++)
								mem.put(di++, input.get(i));
							input = input.range(length);
							regs[eax] = length;
						} else if (regs[eax] == 4) { // write
							var length = regs[edx];
							var si = index(regs[ecx]);
							var bs = new byte[length];
							for (var i = 0; i < length; i++)
								bs[i] = mem.get(si++);
							output.sink(Bytes.of(bs));
						} else
							Fail.t();
					else
						Fail.t();
					break;
				case JE:
					if (c == 0)
						eip = labels.get(source0);
					break;
				case JMP:
					eip = labels.get(source0);
					break;
				case JG:
					if (0 < c)
						eip = labels.get(source0);
					break;
				case JGE:
					if (0 <= c)
						eip = labels.get(source0);
					break;
				case JL:
					if (c < 0)
						eip = labels.get(source0);
					break;
				case JLE:
					if (c <= 0)
						eip = labels.get(source0);
					break;
				case JNE:
					if (c != 0)
						eip = labels.get(source0);
					break;
				case JNZ:
					if (c != 0)
						eip = labels.get(source0);
					break;
				case JZ:
					if (c == 0)
						eip = labels.get(source0);
					break;
				case LABEL:
					break;
				case LEA:
					assign.sink(address((OpMem) op1));
					break;
				case MOV:
					assign.sink(source1);
					break;
				case MOVSB:
					movsb();
					break;
				case MOVSD:
					movsd();
					break;
				case OR:
					assign.sink(source0 | source1);
					break;
				case POP:
					assign.sink(pop());
					break;
				case PUSH:
					push(source0);
					break;
				case REP:
					var movs = instructions.get(eip++).insn;
					Runnable r = movs == Insn.MOVSB ? this::movsb : movs == Insn.MOVSD ? this::movsd : Fail.t();
					while (0 < regs[ecx]--)
						r.run();
					break;
				case RET:
					eip = pop();
					break;
				case SETE:
					assign.sink(c == 0 ? 1 : 0);
					break;
				case SETG:
					assign.sink(0 < c ? 1 : 0);
					break;
				case SETGE:
					assign.sink(0 <= c ? 1 : 0);
					break;
				case SETL:
					assign.sink(c < 0 ? 1 : 0);
					break;
				case SETLE:
					assign.sink(c <= 0 ? 1 : 0);
					break;
				case SETNE:
					assign.sink(c != 0 ? 1 : 0);
					break;
				case SUB:
					assign.sink(source0 - source1);
					break;
				case XOR:
					assign.sink(source0 ^ source1);
					break;
				default:
					Fail.t();
				}
			} catch (Exception ex) {
				LogUtil.info(state(instruction));
				throw ex;
			}
		}
	}

	private void movsb() {
		mem.put(index(regs[edi]), mem.get(index(regs[esi])));
		regs[esi]++;
		regs[edi]++;
	}

	private void movsd() {
		mem.putInt(index(regs[edi]), mem.getInt(index(regs[esi])));
		regs[esi] += 4;
		regs[edi] += 4;
	}

	private void push(int value) {
		regs[esp] -= Funp_.integerSize;
		mem.putInt(index(regs[esp]), value);
	}

	private int pop() {
		var i = mem.getInt(index(regs[esp]));
		regs[esp] += Funp_.integerSize;
		return i;
	}

	private int address(OpMem opMem) {
		var br = opMem.baseReg;
		var ir = opMem.indexReg;
		return (int) opMem.disp + (0 <= br ? regs[br] : 0) + (0 <= ir ? regs[ir] * scales[opMem.scale] : 0);
	}

	private int index(int address) {
		if (address < baseCodex)
			return address - diffCode;
		else if (address < baseStackx)
			return address - diffStack;
		else
			return Fail.t("Wild address " + Integer.toHexString(address));
	}

	private String state(Instruction instruction) {
		var sb = new StringBuilder();
		for (var i = 0; i < 8; i++)
			sb.append((i % 2 == 0 ? "\n" : " ") + amd64.regByName.inverse().get(amd64.reg32[i]) + ":" + To.hex8(regs[i]));
		sb.append("\nCMP = " + c);
		sb.append("\nINSTRUCTION = " + dump.dump(instruction));
		return sb.toString();
	}

}
