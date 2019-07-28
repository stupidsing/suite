package suite.assembler;

import static java.lang.Math.min;
import static suite.util.Fail.fail;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import suite.adt.pair.Pair;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpImm;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.funp.Funp_;
import suite.os.Log_;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.primitive.IntInt_Obj;
import suite.primitive.IntRange;
import suite.primitive.LngLng_Obj;
import suite.primitive.LngPrimitives.LngSink;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.LngRange;
import suite.primitive.adt.map.LngIntMap;
import suite.streamlet.FunUtil.Sink;
import suite.util.String_;
import suite.util.To;

public class Amd64Interpret {

	public int codeStart = 0x00000100;
	public BytesBuilder out = new BytesBuilder();

	private LngLng_Obj<LngRange> fl = (s, p) -> LngRange.of(s, s + p);
	private IntInt_Obj<IntRange> fi = (s, p) -> IntRange.of(s, s + p);

	private LngRange baseNull = LngRange.of(0, codeStart);
	private LngRange baseCode = fl.apply(baseNull.e, 0x08000000);
	private LngRange baseData = fl.apply(baseCode.e, 0x08000000);
	private LngRange baseStack = fl.apply(baseData.e, 0x00040000);
	// private LngRange baseEnd = fl.apply(baseStack.e, 0);

	private IntRange posNull = IntRange.of(0, 0);
	private IntRange posCode = fi.apply(posNull.e, 65536);
	private IntRange posData = fi.apply(posCode.e, 262144);
	private IntRange posStack = fi.apply(posData.e, 262144);
	private IntRange posEnd = fi.apply(posStack.e, 0);

	private ByteBuffer mem = ByteBuffer.allocate(posEnd.s);
	private long[] regs = new long[16];
	private int c;

	private Amd64 amd64 = Amd64.me;
	private Amd64Dump dump = new Amd64Dump();
	private int eax = amd64.axReg;
	private int ebx = amd64.bxReg;
	private int ecx = amd64.cxReg;
	private int edx = amd64.dxReg;
	private int esp = amd64.spReg;
	private int esi = amd64.siReg;
	private int edi = amd64.diReg;
	private int eip;

	private int[] scales = { 1, 2, 4, 8, };

	private Sink<Bytes> output = out::append;

	public Amd64Interpret() {
	}

	public int interpret(Pair<List<Instruction>, Bytes> pair, Bytes in) {
		return interpret(pair.k, pair.v, in);
	}

	public int interpret(List<Instruction> instructions, Bytes code, Bytes in) {
		out.clear();

		var io = new Object() {
			private Bytes input = in;

			private int read(int p1, int p2, int p3) {
				var length = min(p3, input.size());
				var di = index(p2);
				for (var i = 0; i < length; i++)
					mem.put(di++, input.get(i));
				input = input.range(length);
				return length;
			}

			private int write(int p1, int p2, int p3) {
				var length = p3;
				var si = index(p2);
				var bs = new byte[length];
				for (var i = 0; i < length; i++)
					bs[i] = mem.get(si++);
				output.f(Bytes.of(bs));
				return length;
			}
		};

		mem.order(ByteOrder.LITTLE_ENDIAN);
		mem.position(posCode.s);
		mem.put(code.bs);
		eip = 0;
		regs[esp] = baseStack.e - 16;

		var labelAddressByInsnIndex = new LngIntMap();

		for (var i = 0; i < instructions.size(); i++) {
			var i_ = i;
			var instruction = instructions.get(i_);
			if (instruction.insn == Insn.LABEL)
				labelAddressByInsnIndex.update(((OpImm) instruction.op0).imm, i0 -> i_ + 1);
		}

		while (true) {
			var eip_ = eip;
			var instruction = instructions.get(eip++);

			if (Boolean.FALSE)
				Log_.info(state(eip_, instruction));

			try {
				Obj_Lng<Operand> fetch = op -> {
					long v0;
					if (op instanceof OpImm)
						v0 = ((OpImm) op).imm;
					else if (op instanceof OpMem)
						v0 = mem.getLong(index(address((OpMem) op)));
					else if (op instanceof OpReg) {
						var reg = ((OpReg) op).reg;
						v0 = regs[reg];
					} else
						v0 = 0;
					return trim(v0, op.size);
				};

				var op0 = instruction.op0;
				var op1 = instruction.op1;
				var source0 = fetch.apply(op0);
				var source1 = fetch.apply(op1);
				int p0, p1, p2, p3;
				long rc;
				LngSink assign;
				Runnable r;

				if (op0 instanceof OpMem)
					assign = assignMemory(address((OpMem) op0), op0.size);
				else if (op0 instanceof OpReg) {
					var reg = ((OpReg) op0).reg;
					if (op0.size == 1)
						assign = i -> regs[reg] = regs[reg] & 0xFFFFFFFFFFFFFF00l | i & 0x00000000000000FFl;
					else if (op0.size == 2)
						assign = i -> regs[reg] = regs[reg] & 0xFFFFFFFFFFFF0000l | i & 0x000000000000FFFFl;
					else if (op0.size == 4)
						assign = i -> regs[reg] = i & 0xFFFFFFFFl;
					else if (op0.size == 8)
						assign = i -> regs[reg] = i;
					else
						assign = null;
				} else
					assign = null;

				switch (instruction.insn) {
				case ADD:
					assign.f(setFlags(source0 + source1));
					break;
				case ALIGN:
					break;
				case AND:
					assign.f(setFlags(source0 & source1));
					break;
				case CALL:
					push(eip);
					eip = labelAddressByInsnIndex.get(source0);
					break;
				case CLD:
					break;
				case CMP:
					c = Long.compare(source0, source1);
					break;
				case CMPSB:
					cmpsb();
					break;
				case CMPSD:
					cmpsd();
					break;
				case DEC:
					assign.f(source0 - 1);
					break;
				case IDIV:
					var n = (regs[edx] << 32) + regs[eax];
					var div = n / source0;
					var mod = n % source0;
					regs[eax] = div;
					regs[edx] = mod;
					break;
				case INC:
					assign.f(source0 + 1);
					break;
				case IMUL:
					assign.f(setFlags(source1 * fetch.apply(instruction.op2)));
					break;
				case INT:
					p0 = (int) (regs[eax] & 0xFF);
					p1 = (int) regs[ebx];
					p2 = (int) regs[ecx];
					p3 = (int) regs[edx];
					if ((byte) source0 == -128)
						if (p0 == 0x01) // exit
							return p1;
						else if (p0 == 0x03)
							rc = io.read(p1, p2, p3);
						else if (p0 == 0x04)
							rc = io.write(p1, p2, p3);
						else if (p0 == 0x5A) { // map
							var size = mem.getInt(index(p1) + 4);
							rc = size < posData.length() ? baseData.s : fail();
						} else
							rc = fail("invalid int 80h call " + regs[eax]);
					else
						rc = fail();
					regs[eax] = rc;
					break;
				case JE:
					if (c == 0)
						eip = labelAddressByInsnIndex.get(source0);
					break;
				case JMP:
					eip = labelAddressByInsnIndex.get(source0);
					break;
				case JG:
					if (0 < c)
						eip = labelAddressByInsnIndex.get(source0);
					break;
				case JGE:
					if (0 <= c)
						eip = labelAddressByInsnIndex.get(source0);
					break;
				case JL:
					if (c < 0)
						eip = labelAddressByInsnIndex.get(source0);
					break;
				case JLE:
					if (c <= 0)
						eip = labelAddressByInsnIndex.get(source0);
					break;
				case JNE:
					if (c != 0)
						eip = labelAddressByInsnIndex.get(source0);
					break;
				case JNZ:
					if (c != 0)
						eip = labelAddressByInsnIndex.get(source0);
					break;
				case JZ:
					if (c == 0)
						eip = labelAddressByInsnIndex.get(source0);
					break;
				case LABEL:
					break;
				case LEA:
					assign.f(address((OpMem) op1));
					break;
				case LOG:
					Log_.info("value = " + To.hex8(source0));
					break;
				case MOV:
					assign.f(source1);
					break;
				case MOVSB:
					movsb();
					break;
				case MOVSD:
					movsd();
					break;
				case MOVSX:
					assign.f(source1);
					break;
				case NEG:
					assign.f(-source0);
					break;
				case NOT:
					assign.f(~source0);
					break;
				case OR:
					assign.f(setFlags(source0 | source1));
					break;
				case POP:
					assign.f(pop());
					break;
				case PUSH:
					push(source0);
					break;
				case REP:
					r = getNextRepeatInsn(instructions);
					while (0 < regs[ecx]--)
						r.run();
					break;
				case REPE:
					r = getNextRepeatInsn(instructions);
					while (0 < regs[ecx]--) {
						r.run();
						if (c != 0)
							break;
					}
					break;
				case REPNE:
					r = getNextRepeatInsn(instructions);
					while (0 < regs[ecx]--) {
						r.run();
						if (c == 0)
							break;
					}
					break;
				case RET:
					eip = (int) pop();
					break;
				case SAL:
					assign.f(source0 << source1);
					break;
				case SAR:
					assign.f(source0 >>> source1);
					break;
				case SETE:
					assign.f(c == 0 ? 1 : 0);
					break;
				case SETG:
					assign.f(0 < c ? 1 : 0);
					break;
				case SETGE:
					assign.f(0 <= c ? 1 : 0);
					break;
				case SETL:
					assign.f(c < 0 ? 1 : 0);
					break;
				case SETLE:
					assign.f(c <= 0 ? 1 : 0);
					break;
				case SETNE:
					assign.f(c != 0 ? 1 : 0);
					break;
				case SHL:
					assign.f(source0 << source1);
					break;
				case SHR:
					assign.f(source0 >> source1);
					break;
				case SUB:
					assign.f(setFlags(source0 - source1));
					break;
				case SYSCALL:
					p0 = (int) (regs[eax] & 0xFF);
					p1 = (int) regs[edi];
					p2 = (int) regs[esi];
					p3 = (int) regs[edx];
					if (p0 == 0x00)
						rc = io.read(p1, p2, p3);
					else if (p0 == 0x01)
						rc = io.write(p1, p2, p3);
					else if (p0 == 0x09) // map
						rc = p2 < posData.length() ? baseData.s : fail();
					else if (p0 == 0x3C) // exit
						return (int) p1;
					else
						rc = fail("invalid syscall " + regs[eax]);
					regs[eax] = rc;
					break;
				case XOR:
					assign.f(setFlags(source0 ^ source1));
					break;
				default:
					fail("unknown instruction " + instruction.insn);
				}
			} catch (Exception ex) {
				Log_.info(state(eip_, instruction));
				throw ex;
			}
		}
	}

	private Runnable getNextRepeatInsn(List<Instruction> instructions) {
		Insn insn = instructions.get(eip++).insn;
		Runnable r = null;
		r = insn == Insn.CMPSB ? this::cmpsb : r;
		r = insn == Insn.CMPSD ? this::cmpsd : r;
		r = insn == Insn.CMPSQ ? this::cmpsq : r;
		r = insn == Insn.MOVSB ? this::movsb : r;
		r = insn == Insn.MOVSD ? this::movsd : r;
		r = insn == Insn.MOVSQ ? this::movsq : r;
		return r != null ? r : fail();
	}

	private void cmpsb() {
		c = Byte.compare(mem.get(index(regs[esi])), mem.get(index(regs[edi])));
		regs[esi]++;
		regs[edi]++;
	}

	private void cmpsd() {
		c = Integer.compare(mem.getInt(index(regs[esi])), mem.getInt(index(regs[edi])));
		regs[esi] += 4;
		regs[edi] += 4;
	}

	private void cmpsq() {
		c = Long.compare(mem.getLong(index(regs[esi])), mem.getLong(index(regs[edi])));
		regs[esi] += 8;
		regs[edi] += 8;
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

	private void movsq() {
		mem.putLong(index(regs[edi]), mem.getLong(index(regs[esi])));
		regs[esi] += 8;
		regs[edi] += 8;
	}

	private void push(long value) {
		regs[esp] -= Funp_.pushSize;
		assignMemory(regs[esp], Funp_.pushSize).f(value);
	}

	private long pop() {
		var i = trim(mem.getLong(index(regs[esp])), Funp_.pushSize);
		regs[esp] += Funp_.pushSize;
		return i;
	}

	private long setFlags(long value) {
		c = Long.compare(value, 0);
		return value;
	}

	private LngSink assignMemory(long address, int size) {
		LngSink assign;
		var index = index(address);
		if (size == 1)
			assign = i -> mem.put(index, (byte) i);
		else if (size == 2)
			assign = i -> mem.putShort(index, (short) i);
		else if (size == 4)
			assign = i -> mem.putInt(index, (int) i);
		else if (size == 8)
			assign = i -> mem.putLong(index, i);
		else
			assign = null;
		return assign;
	}

	private long trim(long i, int size) {
		if (size == 1)
			return (byte) i;
		else if (size == 2)
			return (short) i;
		else if (size == 4)
			return (int) i;
		else if (size == 8)
			return i;
		else
			return i;
	}

	private long address(OpMem opMem) {
		var br = opMem.baseReg;
		var ir = opMem.indexReg;
		return opMem.disp.imm + (0 <= br ? regs[br] : 0) + (0 <= ir ? regs[ir] * scales[opMem.scale] : 0);
	}

	private int index(long address) {
		if (address < baseCode.e)
			return posCode.s + (int) (address - baseCode.s);
		else if (address < baseData.e)
			return posData.s + (int) (address - baseData.s);
		else if (address < baseStack.e)
			return posStack.s + (int) (address - baseStack.s);
		else
			return fail("address gone wild: " + Long.toHexString(address));
	}

	private String state(int eip, Instruction instruction) {
		return String_.build(sb -> {
			for (var i = 0; i < 8; i++)
				sb.append((i % 2 == 0 ? "\n" : " ") //
						+ amd64.regByName.inverse().get(amd64.reg32[i]) //
						+ ":" + To.hex8(regs[i]));
			sb.append("\nCMP = " + c);
			sb.append("\n[" + To.hex8(eip) + "] INSTRUCTION = " + dump.dump(instruction));
		});
	}

}
