package suite.assembler;

import static java.lang.Math.min;
import static primal.statics.Fail.fail;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import primal.Verbs.Build;
import primal.Verbs.Format;
import primal.adt.Pair;
import primal.fp.Funs.Sink;
import primal.os.Log_;
import primal.primitive.IntInt_Obj;
import primal.primitive.LngLng_Obj;
import primal.primitive.LngPrim.LngSink;
import primal.primitive.LngPrim.Obj_Lng;
import primal.primitive.adt.Bytes;
import primal.primitive.adt.Bytes.BytesBuilder;
import primal.primitive.adt.IntRange;
import primal.primitive.adt.LngRange;
import primal.primitive.adt.map.LngIntMap;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpImm;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpNone;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.OpRemark;
import suite.assembler.Amd64.Operand;

public class Amd64Interpret extends Amd64Cfg {

	public int codeStart = 0x00800000;
	public BytesBuilder out = new BytesBuilder();

	private LngLng_Obj<LngRange> fl = (s, p) -> LngRange.of(s, s + p);
	private IntInt_Obj<IntRange> fi = (s, p) -> IntRange.of(s, s + p);

	private LngRange baseNull = LngRange.of(0, codeStart);
	private LngRange baseCode = fl.apply(baseNull.e, 65536);
	private LngRange baseData = fl.apply(baseCode.e, 262144);
	private LngRange baseStack = fl.apply(baseData.e, 262144);
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
	private Amd64Dump dump;
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

	public Amd64Interpret(boolean isLongMode) {
		super(new Amd64Cfg(isLongMode));
		dump = new Amd64Dump(isLongMode);
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
					assign = switch (op0.size) {
					case 1 -> i -> regs[reg] = regs[reg] & 0xFFFFFFFFFFFFFF00l | i & 0x00000000000000FFl;
					case 2 -> i -> regs[reg] = regs[reg] & 0xFFFFFFFFFFFF0000l | i & 0x000000000000FFFFl;
					case 4 -> i -> regs[reg] = i & 0xFFFFFFFFl;
					case 8 -> i -> regs[reg] = i;
					default -> null;
					};
				} else
					assign = null;

				switch (instruction.insn) {
				case ADD -> assign.f(setFlags(source0 + source1));
				case ALIGN -> jumpIf(false, 0);
				case AND -> assign.f(setFlags(source0 & source1));
				case CALL -> {
					push(eip);
					jumpIf(true, labelAddressByInsnIndex.get(source0));
				}
				case CLD -> jumpIf(false, 0);
				case CMP -> c = Long.compare(source0, source1);
				case CMPSB -> cmpsb();
				case CMPSD -> cmpsd();
				case DEC -> assign.f(source0 - 1);
				case HLT -> fail(op0 instanceof OpRemark ? ((OpRemark) op0).remark : null);
				case IDIV -> {
					var n = (regs[edx] << 32) + regs[eax];
					var div = n / source0;
					var mod = n % source0;
					regs[eax] = div;
					regs[edx] = mod;
				}
				case INC -> assign.f(source0 + 1);
				case IMUL -> assign.f(setFlags(instruction.op2 instanceof OpNone //
						? source0 * source1 //
						: source1 * fetch.apply(instruction.op2)));
				case INT -> {
					p0 = (int) (regs[eax] & 0xFF);
					p1 = (int) regs[ebx];
					p2 = (int) regs[ecx];
					p3 = (int) regs[edx];
					if (p0 == 0x01)
						return p1;
					else
						rc = (byte) source0 == -128 ? switch (p0) {
						case 0x03 -> io.read(p1, p2, p3);
						case 0x04 -> io.write(p1, p2, p3);
						case 0x5A -> { // map
							var size = mem.getInt(index(p1) + 4);
							yield size < posData.length() ? baseData.s : fail();
						}
						default -> fail("invalid int 80h call " + regs[eax]);
						} : fail();
					regs[eax] = rc;
				}
				case JE -> jumpIf(c == 0, labelAddressByInsnIndex.get(source0));
				case JMP -> jumpIf(true, labelAddressByInsnIndex.get(source0));
				case JG -> jumpIf(0 < c, labelAddressByInsnIndex.get(source0));
				case JGE -> jumpIf(0 <= c, labelAddressByInsnIndex.get(source0));
				case JL -> jumpIf(c < 0, labelAddressByInsnIndex.get(source0));
				case JLE -> jumpIf(c <= 0, labelAddressByInsnIndex.get(source0));
				case JNE -> jumpIf(c != 0, labelAddressByInsnIndex.get(source0));
				case JNZ -> jumpIf(c != 0, labelAddressByInsnIndex.get(source0));
				case JZ -> jumpIf(c == 0, labelAddressByInsnIndex.get(source0));
				case LABEL -> jumpIf(false, 0);
				case LEA -> assign.f(address((OpMem) op1));
				case LOG -> {
					if (op0 instanceof OpRemark)
						Log_.info(((OpRemark) op0).remark + " = " + Format.hex8(source1));
					else
						Log_.info("value = " + Format.hex8(source0));
				}
				case MOV -> assign.f(source1);
				case MOVSB -> movsb();
				case MOVSD -> movsd();
				case MOVSX -> assign.f(source1);
				case MOVSXD -> assign.f(source1);
				case NEG -> assign.f(-source0);
				case NOP -> jumpIf(false, 0);
				case NOT -> assign.f(~source0);
				case OR -> assign.f(setFlags(source0 | source1));
				case POP -> assign.f(pop());
				case PUSH -> push(source0);
				case RDTSC, RDTSCP -> {
					var ts = System.currentTimeMillis();
					regs[eax] = (int) ts;
					regs[edx] = (int) (ts >> 32);
				}
				case REMARK -> jumpIf(false, 0);
				case REP -> {
					r = getNextRepeatInsn(instructions);
					while (0 < regs[ecx]--)
						r.run();
				}
				case REPE -> {
					r = getNextRepeatInsn(instructions);
					while (0 < regs[ecx]--) {
						r.run();
						if (c != 0)
							break;
					}
				}
				case REPNE -> {
					r = getNextRepeatInsn(instructions);
					while (0 < regs[ecx]--) {
						r.run();
						if (c == 0)
							break;
					}
				}
				case RET -> eip = (int) pop();
				case SAL -> assign.f(source0 << source1);
				case SAR -> assign.f(source0 >>> source1);
				case SETE -> assign.f(c == 0 ? 1 : 0);
				case SETG -> assign.f(0 < c ? 1 : 0);
				case SETGE -> assign.f(0 <= c ? 1 : 0);
				case SETL -> assign.f(c < 0 ? 1 : 0);
				case SETLE -> assign.f(c <= 0 ? 1 : 0);
				case SETNE -> assign.f(c != 0 ? 1 : 0);
				case SHL -> assign.f(source0 << source1);
				case SHR -> assign.f(source0 >> source1);
				case SUB -> assign.f(setFlags(source0 - source1));
				case SYSCALL -> {
					p0 = (int) (regs[eax] & 0xFF);
					p1 = (int) regs[edi];
					p2 = (int) regs[esi];
					p3 = (int) regs[edx];
					if (p0 == 0x3C)
						return p1;
					else
						regs[eax] = switch (p0) {
						case 0x00 -> io.read(p1, p2, p3);
						case 0x01 -> io.write(p1, p2, p3);
						case 0x09 -> p2 < posData.length() ? baseData.s : fail(); // map
						default -> fail("invalid syscall " + regs[eax]);
						};
				}
				case XOR -> assign.f(setFlags(source0 ^ source1));
				default -> fail("unknown instruction " + instruction.insn);
				}
			} catch (Exception ex) {
				Log_.info(state(eip_, instruction));
				throw ex;
			}
		}
	}

	private Runnable getNextRepeatInsn(List<Instruction> instructions) {
		return switch (instructions.get(eip++).insn) {
		case CMPSB -> this::cmpsb;
		case CMPSD -> this::cmpsd;
		case CMPSQ -> this::cmpsq;
		case MOVSB -> this::movsb;
		case MOVSD -> this::movsd;
		case MOVSQ -> this::movsq;
		default -> fail();
		};
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
		regs[esp] -= pushSize;
		assignMemory(regs[esp], pushSize).f(value);
	}

	private long pop() {
		var i = trim(mem.getLong(index(regs[esp])), pushSize);
		regs[esp] += pushSize;
		return i;
	}

	private void jumpIf(boolean b, int target) {
		if (b)
			eip = target;
	}

	private long setFlags(long value) {
		c = Long.compare(value, 0);
		return value;
	}

	private LngSink assignMemory(long address, int size) {
		var index = index(address);
		return switch (size) {
		case 1 -> i -> mem.put(index, (byte) i);
		case 2 -> i -> mem.putShort(index, (short) i);
		case 4 -> i -> mem.putInt(index, (int) i);
		case 8 -> i -> mem.putLong(index, i);
		default -> null;
		};
	}

	private long trim(long i, int size) {
		return switch (size) {
		case 1 -> (byte) i;
		case 2 -> (short) i;
		case 4 -> (int) i;
		case 8 -> i;
		default -> i;
		};
	}

	private long address(OpMem opMem) {
		var br = opMem.baseReg;
		var ir = opMem.indexReg;
		return opMem.disp.imm + (0 <= br ? regs[br] : 0) + (0 <= ir ? regs[ir] * scales[opMem.scale] : 0);
	}

	private int index(long address) {
		if (baseCode.s <= address && address < baseCode.e)
			return posCode.s + (int) (address - baseCode.s);
		else if (baseData.s <= address && address < baseData.e)
			return posData.s + (int) (address - baseData.s);
		else if (baseStack.s <= address && address < baseStack.e)
			return posStack.s + (int) (address - baseStack.s);
		else
			return fail("address gone wild: " + Long.toHexString(address));
	}

	private String state(int eip, Instruction instruction) {
		return Build.string(sb -> {
			for (var i = 0; i < 8; i++)
				sb.append((i % 2 == 0 ? "\n" : " ") //
						+ amd64.regByName.inverse().get(amd64.reg32[i]) //
						+ ":" + Format.hex8(regs[i]));
			sb.append("\nCMP = " + c);
			sb.append("\n[" + Format.hex8(eip) + "] INSTRUCTION = " + dump.dump(instruction));
		});
	}

}
