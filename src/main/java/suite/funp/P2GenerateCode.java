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
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpTree;
import suite.funp.P1.FunpAllocStack;
import suite.funp.P1.FunpAssign;
import suite.funp.P1.FunpFramePointer;
import suite.funp.P1.FunpInvoke;
import suite.funp.P1.FunpMemory;
import suite.funp.P1.FunpSaveEbp;
import suite.funp.P1.FunpSaveRegisters;
import suite.funp.P1.FunpSeq;
import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;
import suite.primitive.Bytes;
import suite.streamlet.Read;
import suite.util.FunUtil2.Sink2;

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
	private OpReg ecx = amd64.reg("ECX");
	private OpReg ebp = amd64.reg("EBP");
	private OpReg esp = amd64.reg("ESP");

	private OpReg[] stack = new OpReg[] { //
			amd64.reg("EAX"), //
			amd64.reg("EBX"), //
			amd64.reg("ESI"), };

	private Map<Operator, Insn> insnByOp = Read.<Operator, Insn>empty2() //
			.cons(TermOp.PLUS__, Insn.ADD) //
			.cons(TermOp.MINUS_, Insn.SUB) //
			.cons(TermOp.MULT__, Insn.IMUL) //
			.toMap();

	private List<Instruction> instructions = new ArrayList<>();

	public Bytes compile(Funp funp, int offset) {
		Amd64Assembler asm = new Amd64Assembler();
		compileReg_(0, 0, funp);
		return asm.assemble(offset, instructions);
	}

	private void compileReg_( //
			int r, // register stack pointer
			int spd, // = ESP - EBP
			Funp n0) {
		Opt<Operand> oper;
		OpReg r0 = stack[r];

		if (n0 instanceof FunpAllocStack) {
			FunpAllocStack n1 = (FunpAllocStack) n0;
			int size = n1.size;
			Operand imm = amd64.imm(size);
			instructions.add(amd64.instruction(Insn.SUB, esp, imm));
			compileReg_(r, spd - size, n1.expr.apply(FunpMemory.of(new FunpFramePointer(), spd, spd + size)));
			instructions.add(amd64.instruction(Insn.ADD, esp, imm));
		} else if (n0 instanceof FunpAssign) {
			FunpAssign n1 = (FunpAssign) n0;
			FunpMemory m0 = n1.memory;
			Funp value = n1.value;
			compileAssign(r, spd, m0, value);
		} else if (n0 instanceof FunpFixed)
			;
		else if (n0 instanceof FunpIf) {
			Operand elseLabel = amd64.imm(0, ps);
			Operand endLabel = amd64.imm(0, ps);
			compileReg_(r, spd, ((FunpIf) n0).if_);
			instructions.add(amd64.instruction(Insn.OR, r0, r0));
			instructions.add(amd64.instruction(Insn.JZ, elseLabel));
			compileReg_(r, spd, ((FunpIf) n0).then);
			instructions.add(amd64.instruction(Insn.JMP, endLabel));
			instructions.add(amd64.instruction(Insn.LABEL, elseLabel));
			compileReg_(r, spd, ((FunpIf) n0).else_);
			instructions.add(amd64.instruction(Insn.LABEL, endLabel));
		} else if (n0 instanceof FunpInvoke)
			compileInvoke(r, spd, //
					(FunpInvoke) n0, is, //
					(r_, disp) -> instructions.add(amd64.instruction(Insn.MOV, r0, amd64.mem(r_, disp, is))));
		else if (n0 instanceof FunpSaveEbp) {
			instructions.add(amd64.instruction(Insn.PUSH, ebp));
			compileReg_(r, spd - 4, ((FunpSaveEbp) n0).expr);
			instructions.add(amd64.instruction(Insn.POP, ebp));
		} else if (n0 instanceof FunpSaveRegisters) {
			for (int i = 0; i <= r - 1; i++)
				instructions.add(amd64.instruction(Insn.PUSH, stack[i]));
			compileReg_(r, spd - r * is, ((FunpSaveRegisters) n0).expr);
			for (int i = r - 1; 0 <= i; i--)
				instructions.add(amd64.instruction(Insn.POP, stack[i]));
		} else if (n0 instanceof FunpSeq)
			for (Funp expr : ((FunpSeq) n0).exprs)
				compileReg_(r, spd, expr);
		else if (n0 instanceof FunpTree) {
			FunpTree n1 = (FunpTree) n0;
			Operator operator = n1.operator;
			int sp1 = r + 1;
			if (operator.getAssoc() == Assoc.RIGHT) {
				compileReg_(r, spd, n1.right);
				compileReg_(sp1, spd, n1.left);
			} else {
				compileReg_(r, spd, n1.left);
				compileReg_(sp1, spd, n1.right);
			}
			instructions.add(amd64.instruction(insnByOp.get(operator), r0, stack[sp1]));
		} else if (!(oper = compileOp(r, n0)).isEmpty())
			instructions.add(amd64.instruction(Insn.MOV, r0, oper.get()));
		else
			throw new RuntimeException("cannot generate code for " + n0);
	}

	private void compileAssign(int r, int spd, FunpMemory target, Funp source) {
		OpReg r0 = stack[r];
		int size = target.size();

		compileReg_(r, spd, target.pointer);

		if (size <= is) {
			Operand mem = amd64.mem(r0, target.start, size);
			compileReg_(r + 1, spd, source);
			instructions.add(amd64.instruction(Insn.MOV, mem, stack[r + 1]));
		} else if (source instanceof FunpInvoke)
			compileInvoke(r, spd, //
					(FunpInvoke) source, size, //
					(r_, disp) -> compileMove(r, r0, target.start, r_, disp, target.size()));
		else if (source instanceof FunpLambda) {
			Operand lambdaLabel = amd64.imm(0, ps);
			Operand endLabel = amd64.imm(0, ps);
			instructions.add(amd64.instruction(Insn.JMP, endLabel));
			instructions.add(amd64.instruction(Insn.LABEL, lambdaLabel));
			instructions.add(amd64.instruction(Insn.PUSH, ebp));
			instructions.add(amd64.instruction(Insn.MOV, ebp, esp));
			compileReg_(0, 4, ((FunpLambda) source).expr);
			instructions.add(amd64.instruction(Insn.POP, ebp));
			instructions.add(amd64.instruction(Insn.RET));
			instructions.add(amd64.instruction(Insn.LABEL, endLabel));
			instructions.add(amd64.instruction(Insn.MOV, amd64.mem(r0, target.start, ps), ebp));
			instructions.add(amd64.instruction(Insn.MOV, amd64.mem(r0, target.start + ps, ps), lambdaLabel));
		} else if (source instanceof FunpMemory)
			compileMove(r, spd, target, (FunpMemory) source);
		else
			throw new RuntimeException();
	}

	private void compileInvoke(int r, int spd, FunpInvoke invoke, int size, Sink2<OpReg, Integer> onResult) {
		OpReg r0 = stack[r];
		Funp lambda = invoke.lambda;

		if (lambda instanceof FunpMemory) {
			FunpMemory memory = (FunpMemory) lambda;
			Funp frame = memory.range(0, ps);
			Funp ip = memory.range(ps, ps + ps);
			compileReg_(r, spd, frame);
			compileReg_(r + 1, spd, ip);
			instructions.add(amd64.instruction(Insn.MOV, ebp, r0));
			instructions.add(amd64.instruction(Insn.CALL, stack[r + 1]));
			onResult.sink2(esp, is - size);
		} else {
			FunpAllocStack n_ = FunpAllocStack.of(ps + ps,
					buffer -> FunpSeq.of(FunpAssign.of(buffer, lambda), FunpInvoke.of(buffer)));
			compileReg_(r, spd, n_);
		}
	}

	private void compileMove(int r, int spd, FunpMemory target, FunpMemory source) {
		int size = target.size();
		if (size == source.size()) {
			int r1 = r + 1;
			compileReg_(r1, spd, source.pointer);
			compileMove(r, stack[r], target.start, stack[r1], source.start, size);
		} else
			throw new RuntimeException();
	}

	private void compileMove(int r, OpReg reg0, int start0, OpReg reg1, int start1, int size) {
		if (size <= 16) {
			int i = 0;

			while (i < size) {
				int s = i + is <= size ? is : 1;
				OpReg reg = 1 < s ? ecx : cl;
				instructions.add(amd64.instruction(Insn.MOV, reg, amd64.mem(reg1, start1 + i, s)));
				instructions.add(amd64.instruction(Insn.MOV, amd64.mem(reg0, start0 + i, s), reg));
				i += s;
			}
		} else {
			doRegs(Insn.PUSH, r - 1, "ECX", "ESI", "EDI");
			instructions.add(amd64.instruction(Insn.LEA, amd64.reg("ESI"), amd64.mem(reg1, start1, 4)));
			instructions.add(amd64.instruction(Insn.LEA, amd64.reg("EDI"), amd64.mem(reg0, start0, 4)));
			instructions.add(amd64.instruction(Insn.MOV, amd64.reg("ECX"), amd64.imm(size / 4, 4)));
			instructions.add(amd64.instruction(Insn.REP));
			instructions.add(amd64.instruction(Insn.MOVSD));
			for (int i = 0; i < size % 4; i++)
				instructions.add(amd64.instruction(Insn.MOVSB));
			instructions.add(amd64.instruction(Insn.CLD));
			instructions.add(amd64.instruction(Insn.POP, amd64.reg("ESI")));
			doRegs(Insn.POP, r - 1, "EDI", "ESI", "ECX");
		}
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

	private void doRegs(Insn insn, int r, String... regs) {
		for (String reg : regs) {
			OpReg opReg = amd64.reg(reg);
			for (int i = 0; i < r; i++)
				if (opReg == stack[i])
					instructions.add(amd64.instruction(insn, opReg));
		}
	}

}
