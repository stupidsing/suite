package suite.assembler;

import java.util.List;

import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpImm;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.funp.Funp_;
import suite.primitive.IntPrimitives.IntSink;
import suite.primitive.adt.map.IntIntMap;

public class Amd64Interpret {

	private int org = 0x08048000;
	private int[] mem = new int[65536];
	private int[] regs = new int[16];
	private int c;

	private Amd64 amd64 = Amd64.me;
	private int eax = amd64.eax.reg;
	private int ebx = amd64.ebx.reg;
	private int esp = amd64.esp.reg;

	private int[] scales = new int[] { 1, 2, 4, 8, };

	public Amd64Interpret() {
		regs[esp] = 0xF0000000;
	}

	public int interpret(List<Instruction> instructions) {
		IntIntMap labels = new IntIntMap();

		for (int i = 0; i < instructions.size(); i++) {
			Instruction instruction = instructions.get(i);
			if (instruction.insn == Insn.LABEL)
				labels.put((int) ((OpImm) instruction.op1).imm, i);
		}

		int ip = 0;

		while (true) {
			Instruction instruction = instructions.get(ip++);
			Operand op0 = instruction.op0;
			Operand op1 = instruction.op1;
			int source0, source1;

			if (op1 instanceof OpImm)
				source0 = (int) ((OpImm) op1).imm;
			else if (op1 instanceof OpMem)
				source0 = mem[address((OpMem) op1)];
			else if (op1 instanceof OpReg)
				source0 = regs[((OpReg) op1).reg];
			else
				source0 = 0;

			IntSink assign;

			if (op0 instanceof OpMem) {
				int address = address((OpMem) op0);
				source1 = mem[address];
				assign = i -> mem[address] = i;
			} else if (op0 instanceof OpReg) {
				int reg = ((OpReg) op1).reg;
				source1 = regs[reg];
				assign = i -> regs[reg] = i;
			} else {
				source1 = 0;
				assign = null;
			}

			switch (instruction.insn) {
			case ADD:
				assign.sink(source0 + source1);
				break;
			case CALL:
				ip = pop();
				break;
			case CMP:
				c = Integer.compare(source0, source1);
				break;
			case INT:
				if (regs[eax] == 0)
					return regs[ebx];
				else
					throw new RuntimeException();
			case JMP:
				ip = jump(ip, op0, source0);
				break;
			case JNE:
				if (c != 0)
					ip = jump(ip, op0, source0);
				break;
			case LEA:
				assign.sink(address((OpMem) op1));
				break;
			case MOV:
				assign.sink(source0);
				break;
			case POP:
				int i = pop();
				assign.sink(i);
				break;
			case PUSH:
				push(source0);
				break;
			case SUB:
				assign.sink(source0 - source1);
				break;
			default:
				throw new RuntimeException(instruction.toString());
			}
		}
	}

	private int jump(int ip, Operand op0, int source0) {
		return op0.size == 1 ? ip + source0 : source0;
	}

	private void push(int source0) {
		esp -= Funp_.integerSize;
		mem[regs[esp] + org] = source0;
	}

	private int pop() {
		int i = mem[regs[esp] + org];
		esp += Funp_.integerSize;
		return i;
	}

	private int address(OpMem opMem) {
		return (int) opMem.disp //
				+ (0 <= opMem.baseReg ? regs[opMem.baseReg] : 0) //
				+ (0 <= opMem.indexReg ? regs[opMem.indexReg] * scales[opMem.scale] : 0) //
				- org;
	}

}
