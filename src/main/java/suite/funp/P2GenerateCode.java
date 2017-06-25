package suite.funp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import suite.adt.Opt;
import suite.assembler.Amd64;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.assembler.Amd64Assembler;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpFixed;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpTree;
import suite.funp.P1.FunpAllocStack;
import suite.funp.P1.FunpAssign;
import suite.funp.P1.FunpFramePointer;
import suite.funp.P1.FunpInvoke;
import suite.funp.P1.FunpMemory;
import suite.funp.P1.FunpRoutine;
import suite.funp.P1.FunpSaveFramePointer;
import suite.funp.P1.FunpSaveRegisters;
import suite.funp.P2.FunpInvokeMemory;
import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;
import suite.primitive.Bytes;
import suite.streamlet.Read;

/**
 * Hindley-Milner type inference.
 *
 * @author ywsing
 */
public class P2GenerateCode {

	private int is = Funp_.integerSize;
	private int ps = Funp_.pointerSize;

	private Amd64 amd64 = new Amd64();
	private OpReg cl = amd64.reg("CL");
	private OpReg eax = amd64.reg("EAX");
	private OpReg ecx = amd64.reg("ECX");
	private OpReg ebp = amd64.reg("EBP");
	private OpReg esp = amd64.reg("ESP");

	private OpReg[] stack = new OpReg[] { //
			amd64.reg("EAX"), //
			amd64.reg("EBX"), //
			amd64.reg("ESI"), };

	private Map<Operator, Insn> insnByOp = Read.<Operator, Insn> empty2() //
			.cons(TermOp.PLUS__, Insn.ADD) //
			.cons(TermOp.MINUS_, Insn.SUB) //
			.cons(TermOp.MULT__, Insn.IMUL) //
			.toMap();

	private List<Instruction> instructions = new ArrayList<>();

	private interface CompileSink {
		public void compile( //
				int sp, // register stack pointer
				int fd, // = ESP - EBP
				Funp node);
	}

	public Bytes compile(Funp funp, int offset) {
		Amd64Assembler asm = new Amd64Assembler();
		compileReg(0, 0, funp);
		return asm.assemble(offset, instructions);
	}

	private void compile(int sp, int fd, CompileSink c, Funp n0) {
		OpReg r0 = stack[sp];

		if (n0 instanceof FunpAllocStack) {
			FunpAllocStack n1 = (FunpAllocStack) n0;
			int size = n1.size;
			Operand imm = amd64.imm(size);
			instructions.add(amd64.instruction(Insn.SUB, esp, imm));
			compile(sp, fd - size, c, n1.expr.apply(FunpMemory.of(new FunpFramePointer(), fd, fd + size)));
			instructions.add(amd64.instruction(Insn.ADD, esp, imm));
		} else if (n0 instanceof FunpAssign) {
			FunpAssign n1 = (FunpAssign) n0;
			compileAssign(sp, fd, n1.memory, n1.value);
			compile(sp, fd, c, n1.expr);
		} else if (n0 instanceof FunpIf) {
			Operand elseLabel = amd64.imm(0, ps);
			Operand endLabel = amd64.imm(0, ps);
			compile(sp, fd, c, ((FunpIf) n0).if_);
			instructions.add(amd64.instruction(Insn.OR, r0, r0));
			instructions.add(amd64.instruction(Insn.JZ, elseLabel));
			compile(sp, fd, c, ((FunpIf) n0).then);
			instructions.add(amd64.instruction(Insn.JMP, endLabel));
			instructions.add(amd64.instruction(Insn.LABEL, elseLabel));
			compile(sp, fd, c, ((FunpIf) n0).else_);
			instructions.add(amd64.instruction(Insn.LABEL, endLabel));
		} else if (n0 instanceof FunpSaveFramePointer) {
			instructions.add(amd64.instruction(Insn.PUSH, ebp));
			compile(sp, fd - 4, c, ((FunpSaveFramePointer) n0).expr);
			instructions.add(amd64.instruction(Insn.POP, ebp));
		} else if (n0 instanceof FunpSaveRegisters) {
			for (int i = 0; i <= sp - 1; i++)
				instructions.add(amd64.instruction(Insn.PUSH, stack[i]));
			compile(sp, fd - sp * is, c, ((FunpSaveRegisters) n0).expr);
			for (int i = sp - 1; 0 <= i; i--)
				instructions.add(amd64.instruction(Insn.POP, stack[i]));
		} else
			c.compile(sp, fd, n0);
	}

	private void compileAssign(int sp, int fd, FunpMemory target, Funp source) {
		compile(sp, fd, (sp_, fd_, n_) -> compileAssign_(sp_, fd_, target, n_), source);
	}

	private void compileAssign_(int sp, int fd, FunpMemory target, Funp source) {
		int sp1 = sp + 1;
		OpReg r0 = stack[sp];
		int size = target.size();

		compileReg(sp, fd, target.pointer);

		if (size <= is) {
			Operand mem = amd64.mem(r0, target.start, size);
			compileReg(sp1, fd, source);
			instructions.add(amd64.instruction(Insn.MOV, mem, stack[sp1]));
		} else if (source instanceof FunpMemory)
			compileMove(sp, fd, target, (FunpMemory) source);
		else if (source instanceof FunpRoutine) {
			Operand lambdaLabel = amd64.imm(0, ps);
			Operand endLabel = amd64.imm(0, ps);
			instructions.add(amd64.instruction(Insn.JMP, endLabel));
			instructions.add(amd64.instruction(Insn.LABEL, lambdaLabel));
			instructions.add(amd64.instruction(Insn.PUSH, ebp));
			instructions.add(amd64.instruction(Insn.MOV, ebp, esp));
			compileReg(0, 4, ((FunpRoutine) source).expr);
			instructions.add(amd64.instruction(Insn.POP, ebp));
			instructions.add(amd64.instruction(Insn.RET));
			instructions.add(amd64.instruction(Insn.LABEL, endLabel));
			instructions.add(amd64.instruction(Insn.MOV, amd64.mem(r0, target.start, ps), ebp));
			instructions.add(amd64.instruction(Insn.MOV, amd64.mem(r0, target.start + ps, ps), lambdaLabel));
		} else
			throw new RuntimeException("cannot assign from " + source);
	}

	private void compileMove(int sp, int fd, FunpMemory target, FunpMemory source) {
		int size = target.size();
		if (size == source.size()) {
			int r1 = sp + 1;
			compileReg(r1, fd, source.pointer);
			compileMove(sp, stack[sp], target.start, stack[r1], source.start, size);
		} else
			throw new RuntimeException();
	}

	private void compileMove(int sp, OpReg r0, int start0, OpReg r1, int start1, int size) {
		if (size <= 16)
			saveRegs(sp - 1, () -> {
				int i = 0;
				while (i < size) {
					int s = i + is <= size ? is : 1;
					OpReg r = 1 < s ? ecx : cl;
					instructions.add(amd64.instruction(Insn.MOV, r, amd64.mem(r1, start1 + i, s)));
					instructions.add(amd64.instruction(Insn.MOV, amd64.mem(r0, start0 + i, s), r));
					i += s;
				}
				;
			}, "ECX");
		else
			saveRegs(sp - 1, () -> {
				instructions.add(amd64.instruction(Insn.LEA, amd64.reg("ESI"), amd64.mem(r1, start1, 4)));
				instructions.add(amd64.instruction(Insn.LEA, amd64.reg("EDI"), amd64.mem(r0, start0, 4)));
				instructions.add(amd64.instruction(Insn.MOV, ecx, amd64.imm(size / 4, 4)));
				instructions.add(amd64.instruction(Insn.CLD));
				instructions.add(amd64.instruction(Insn.REP));
				instructions.add(amd64.instruction(Insn.MOVSD));
				for (int i = 0; i < size % 4; i++)
					instructions.add(amd64.instruction(Insn.MOVSB));
				instructions.add(amd64.instruction(Insn.POP, amd64.reg("ESI")));
			}, "ECX", "ESI", "EDI");
	}

	private void compileReg(int sp, int fd, Funp n0) {
		compile(sp, fd, this::compileReg_, n0);
	}

	private void compileReg_(int sp, int fd, Funp n0) {
		Opt<Operand> oper;
		OpReg r0 = stack[sp];

		if (n0 instanceof FunpFixed)
			;
		else if (n0 instanceof FunpInvoke) {
			Funp lambda = ((FunpInvoke) n0).lambda;
			Funp n1;
			if (lambda instanceof FunpMemory)
				n1 = FunpInvokeMemory.of((FunpMemory) lambda);
			else
				n1 = FunpAllocStack.of(ps + ps, buffer -> FunpAssign.of(buffer, lambda, FunpInvokeMemory.of(buffer)));
			compileReg(sp, fd, n1);
		} else if (n0 instanceof FunpInvokeMemory) {
			FunpMemory memory = ((FunpInvokeMemory) n0).memory;
			Funp frame = memory.range(0, ps);
			Funp ip = memory.range(ps, ps + ps);
			int sp1 = sp + 1;

			compileReg(sp, fd, frame);
			compileReg(sp1, fd, ip);
			instructions.add(amd64.instruction(Insn.MOV, ebp, r0));
			instructions.add(amd64.instruction(Insn.CALL, stack[sp1]));
			instructions.add(amd64.instruction(Insn.MOV, r0, eax));
		} else if (n0 instanceof FunpTree) {
			FunpTree n1 = (FunpTree) n0;
			Operator operator = n1.operator;
			int sp1 = sp + 1;
			if (operator.getAssoc() == Assoc.RIGHT) {
				compileReg(sp, fd, n1.right);
				compileReg(sp1, fd, n1.left);
			} else {
				compileReg(sp, fd, n1.left);
				compileReg(sp1, fd, n1.right);
			}
			instructions.add(amd64.instruction(insnByOp.get(operator), r0, stack[sp1]));
		} else if (!(oper = compileOp(sp, n0)).isEmpty())
			instructions.add(amd64.instruction(Insn.MOV, r0, oper.get()));
		else
			throw new RuntimeException("cannot generate code for " + n0);
	}

	private Opt<Operand> compileOp(int sp, Funp n0) {
		if (n0 instanceof FunpBoolean)
			return Opt.of(amd64.imm(((FunpBoolean) n0).b ? 1 : 0, Funp_.booleanSize));
		else if (n0 instanceof FunpMemory) {
			FunpMemory n1 = (FunpMemory) n0;
			int size = n1.size();
			return compileOpReg(sp, n1.pointer) //
					.filter(op -> size <= Funp_.pointerSize) //
					.map(op -> amd64.mem(op, n1.start, size));
		} else if (n0 instanceof FunpNumber)
			return Opt.of(amd64.imm(((FunpNumber) n0).i, Funp_.integerSize));
		else
			return compileOpReg(sp, n0).map(op -> op);
	}

	private Opt<OpReg> compileOpReg(int sp, Funp n0) {
		if (n0 instanceof FunpFramePointer)
			return Opt.of(ebp);
		else
			return Opt.none();
	}

	private void saveRegs(int sp, Runnable runnable, String... regs) {
		doRegs(sp - 1, Insn.PUSH, regs);
		runnable.run();
		doRegs(sp - 1, Insn.POP, regs);
	}

	private void doRegs(int sp, Insn insn, String... regs) {
		for (String reg : regs) {
			OpReg opReg = amd64.reg(reg);
			for (int i = 0; i < sp; i++)
				if (opReg == stack[i])
					instructions.add(amd64.instruction(insn, opReg));
		}
	}

}
