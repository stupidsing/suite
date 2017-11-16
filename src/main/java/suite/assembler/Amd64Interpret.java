package suite.assembler;

import java.util.List;

import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpImm;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.funp.Funp_;
import suite.os.LogUtil;
import suite.primitive.IntPrimitives.IntSink;
import suite.primitive.Ints;
import suite.primitive.adt.map.IntIntMap;
import suite.util.FunUtil.Sink;
import suite.util.To;

public class Amd64Interpret {

	private int offset;
	private int[] mem = new int[65536];
	private int[] regs = new int[16];
	private int c;

	private Amd64 amd64 = Amd64.me;
	private Amd64Dump dump = new Amd64Dump();
	private int eax = amd64.eax.reg;
	private int ebx = amd64.ebx.reg;
	private int ecx = amd64.ecx.reg;
	private int edx = amd64.edx.reg;
	private int esp = amd64.esp.reg;

	private int[] scales = new int[] { 1, 2, 4, 8, };

	private Ints input;
	private Sink<Ints> output = System.out::println;

	public Amd64Interpret() {
		this(Ints.of());
	}

	public Amd64Interpret(Ints input) {
		this.input = input;
		offset = (regs[esp] = 0xF0000000) - mem.length;
	}

	public int interpret(List<Instruction> instructions) {
		IntIntMap labels = new IntIntMap();

		for (int i = 0; i < instructions.size(); i++) {
			Instruction instruction = instructions.get(i);
			if (instruction.insn == Insn.LABEL)
				labels.put((int) ((OpImm) instruction.op0).imm, i + 1);
		}

		int ip = 0;

		while (true) {
			Instruction instruction = instructions.get(ip++);

			if (Boolean.FALSE)
				LogUtil.info(state(instruction));

			try {
				Operand op0 = instruction.op0;
				Operand op1 = instruction.op1;
				int source0, source1;
				IntSink assign;

				if (op0 instanceof OpImm) {
					source0 = (int) ((OpImm) op0).imm;
					assign = null;
				} else if (op0 instanceof OpMem) {
					int index = index(address((OpMem) op0));
					source0 = mem[index];
					assign = i -> mem[index] = i;
				} else if (op0 instanceof OpReg) {
					int reg = ((OpReg) op0).reg;
					source0 = regs[reg];
					assign = i -> {
						regs[reg] = i;
					};
				} else {
					source0 = 0;
					assign = null;
				}

				if (op1 instanceof OpImm)
					source1 = (int) ((OpImm) op1).imm;
				else if (op1 instanceof OpMem)
					source1 = mem[index(address((OpMem) op1))];
				else if (op1 instanceof OpReg)
					source1 = regs[((OpReg) op1).reg];
				else
					source1 = 0;

				switch (instruction.insn) {
				case ADD:
					assign.sink(source0 + source1);
					break;
				case CALL:
					push(ip);
					ip = labels.get(source0);
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
					if (regs[eax] == 1)
						return regs[ebx];
					else if (regs[eax] == 3) {
						int length = regs[eax] = Math.min(regs[edx] / 4, input.size());
						for (int i = 0; i < length; i++)
							mem[index(regs[ecx]) + i] = input.get(i);
						input = input.range(length);
					} else if (regs[eax] == 4)
						output.sink(Ints.of(mem, index(regs[ecx]), index(regs[ecx] + regs[edx])));
					else
						throw new RuntimeException();
				case JE:
					if (c == 0)
						ip = labels.get(source0);
					break;
				case JMP:
					ip = labels.get(source0);
					break;
				case JG:
					if (0 < c)
						ip = labels.get(source0);
					break;
				case JGE:
					if (0 <= c)
						ip = labels.get(source0);
					break;
				case JL:
					if (c < 0)
						ip = labels.get(source0);
					break;
				case JLE:
					if (c <= 0)
						ip = labels.get(source0);
					break;
				case JNE:
					if (c != 0)
						ip = labels.get(source0);
					break;
				case LABEL:
					break;
				case LEA:
					assign.sink(address((OpMem) op1));
					break;
				case MOV:
					assign.sink(source1);
					break;
				case POP:
					assign.sink(pop());
					break;
				case PUSH:
					push(source0);
					break;
				case RET:
					ip = pop();
					break;
				case SUB:
					assign.sink(source0 - source1);
					break;
				case XOR:
					assign.sink(source0 ^ source1);
					break;
				default:
					throw new RuntimeException();
				}
			} catch (Exception ex) {
				LogUtil.info(state(instruction));
				throw ex;
			}
		}
	}

	private void push(int value) {
		regs[esp] -= Funp_.integerSize;
		mem[index(regs[esp])] = value;
	}

	private int pop() {
		int i = mem[index(regs[esp])];
		regs[esp] += Funp_.integerSize;
		return i;
	}

	private int address(OpMem opMem) {
		int br = opMem.baseReg;
		int ir = opMem.indexReg;
		return (int) opMem.disp + (0 <= br ? regs[br] : 0) + (0 <= ir ? regs[ir] * scales[opMem.scale] : 0);
	}

	private int index(int address) {
		return (address - offset) / Funp_.integerSize;
	}

	private String state(Instruction instruction) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 8; i++)
			sb.append("\n" + amd64.regsByName.inverse().get(amd64.reg32[i]) + " = " + To.hex8(regs[i]));
		sb.append("\nCMP = " + c);
		sb.append("\nINSTRUCTION = " + dump.dump(instruction));
		return sb.toString();
	}

}
