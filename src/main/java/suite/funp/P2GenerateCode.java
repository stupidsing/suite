package suite.funp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import suite.adt.Opt;
import suite.assembler.Amd64;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.assembler.Amd64Assembler;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpFixed;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpVariable;
import suite.funp.P1.FunpAssign;
import suite.funp.P1.FunpFramePointer;
import suite.funp.P1.FunpInvoke;
import suite.funp.P1.FunpMemory;
import suite.funp.P1.FunpSaveEbp;
import suite.funp.P1.FunpSaveRegisters;
import suite.funp.P1.FunpStack;
import suite.funp.P1.FunpStackPointer;
import suite.node.io.TermOp;
import suite.primitive.Bytes;

/**
 * Hindley-Milner type inference.
 *
 * @author ywsing
 */
public class P2GenerateCode {

	private Amd64 amd64 = new Amd64();
	private OpReg cl = amd64.reg("CL");
	private OpReg ecx = amd64.reg("ECX");
	private OpReg ebp = amd64.reg("EBP");
	private OpReg esp = amd64.reg("ESP");

	private OpReg[] stack = new OpReg[] { //
			amd64.reg("EAX"), //
			amd64.reg("EBX"), //
			amd64.reg("ESI"), };

	private List<Instruction> instructions = new ArrayList<>();

	public Bytes compile(Funp funp, int offset) {
		Amd64Assembler asm = new Amd64Assembler();
		compileReg_(0, funp);
		return asm.assemble(offset, instructions);
	}

	private void compileReg_(int sp, Funp n0) {
		ParseOperator po;
		Opt<Operand> oper;
		OpReg r0 = stack[sp];
		int is = Funp_.integerSize;
		int ps = Funp_.pointerSize;

		if (n0 instanceof FunpAssign) {
			FunpAssign n1 = (FunpAssign) n0;
			FunpMemory m0 = n1.memory;
			Funp value = n1.value;

			int size = m0.size();
			compileReg_(sp, m0.pointer);

			if (size <= is) {
				Operand mem = amd64.mem(r0, m0.start, size);
				compileReg_(sp + 1, value);
				instructions.add(amd64.instruction(Insn.MOV, mem, stack[sp + 1]));
			} else if (value instanceof FunpLambda) {
				Operand lambdaLabel = amd64.imm(0, ps);
				Operand endLabel = amd64.imm(0, ps);
				instructions.add(amd64.instruction(Insn.JMP, endLabel));
				instructions.add(amd64.instruction(Insn.LABEL, lambdaLabel));
				instructions.add(amd64.instruction(Insn.PUSH, ebp));
				instructions.add(amd64.instruction(Insn.MOV, ebp, esp));
				compileReg_(0, ((FunpLambda) value).expr);
				instructions.add(amd64.instruction(Insn.POP, ebp));
				instructions.add(amd64.instruction(Insn.RET));
				instructions.add(amd64.instruction(Insn.LABEL, endLabel));
				instructions.add(amd64.instruction(Insn.MOV, amd64.mem(r0, m0.start, ps), ebp));
				instructions.add(amd64.instruction(Insn.MOV, amd64.mem(r0, m0.start + ps, ps), lambdaLabel));
			} else if (value instanceof FunpMemory) {
				FunpMemory m1 = (FunpMemory) value;
				if (size == m1.size()) {
					int sp1 = sp + 1;
					compileReg_(sp1, m1.pointer);
					OpReg r1 = stack[sp1];
					int i = 0;

					while (i < size) {
						int s = i + is <= size ? is : 1;
						OpReg r = 1 < s ? ecx : cl;
						instructions.add(amd64.instruction(Insn.MOV, r, amd64.mem(r1, m1.start + i, s)));
						instructions.add(amd64.instruction(Insn.MOV, amd64.mem(r1, m0.start + i, s), r));
						i += s;
					}
				} else
					throw new RuntimeException();
			} else
				throw new RuntimeException();
			compileReg_(sp, n1.expr);
		} else if (n0 instanceof FunpFixed)
			;
		else if (n0 instanceof FunpIf) {
			Operand elseLabel = amd64.imm(0, ps);
			Operand endLabel = amd64.imm(0, ps);
			compileReg_(sp, ((FunpIf) n0).if_);
			instructions.add(amd64.instruction(Insn.OR, r0, r0));
			instructions.add(amd64.instruction(Insn.JE, elseLabel));
			compileReg_(sp, ((FunpIf) n0).then);
			instructions.add(amd64.instruction(Insn.JMP, endLabel));
			instructions.add(amd64.instruction(Insn.LABEL, elseLabel));
			compileReg_(sp, ((FunpIf) n0).else_);
			instructions.add(amd64.instruction(Insn.LABEL, endLabel));
		} else if (n0 instanceof FunpInvoke) {
			Funp lambda = ((FunpInvoke) n0).lambda;
			if (lambda instanceof FunpMemory) {
				FunpMemory memory = (FunpMemory) lambda;
				Funp frame = memory.range(0, ps);
				Funp ip = memory.range(ps, ps + ps);
				compileReg_(sp, frame);
				compileReg_(sp + 1, ip);
				instructions.add(amd64.instruction(Insn.MOV, ebp, stack[sp]));
				instructions.add(amd64.instruction(Insn.CALL, stack[sp + 1]));
			} else
				throw new RuntimeException("cannot generate code for " + n0);
		} else if (n0 instanceof FunpSaveEbp) {
			instructions.add(amd64.instruction(Insn.PUSH, ebp));
			compileReg_(sp, ((FunpSaveEbp) n0).expr);
			instructions.add(amd64.instruction(Insn.POP, ebp));
		} else if (n0 instanceof FunpSaveRegisters) {
			for (int i = 0; i <= sp - 1; i++)
				instructions.add(amd64.instruction(Insn.PUSH, stack[i]));
			compileReg_(sp, ((FunpSaveRegisters) n0).expr);
			for (int i = sp - 1; 0 <= i; i--)
				instructions.add(amd64.instruction(Insn.POP, stack[i]));
		} else if (n0 instanceof FunpStack) {
			FunpStack n1 = (FunpStack) n0;
			Operand imm = amd64.imm(n1.size);
			instructions.add(amd64.instruction(Insn.SUB, esp, imm));
			compileReg_(sp, n1.expr);
			instructions.add(amd64.instruction(Insn.ADD, esp, imm));
		} else if ((po = parseOperator(n0)) != null && Objects.equals(po.op, TermOp.PLUS__.name)) {
			int sp1 = sp + 1;
			compileReg_(sp, po.right);
			compileReg_(sp1, po.left);
			instructions.add(amd64.instruction(Insn.ADD, r0, stack[sp1]));
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
		else if (n0 instanceof FunpStackPointer)
			return Opt.of(esp);
		else
			return Opt.none();
	}

	public ParseOperator parseOperator(Funp n0) {
		FunpApply n1 = n0 instanceof FunpApply ? (FunpApply) n0 : null;
		Funp n2 = n1 != null ? n1.value : null;
		FunpApply n3 = n2 instanceof FunpApply ? (FunpApply) n2 : null;
		Funp n4 = n3 != null ? n3.value : null;
		String var = n4 instanceof FunpVariable ? ((FunpVariable) n4).var : null;
		if (var != null) {
			ParseOperator po = new ParseOperator();
			po.op = var;
			po.left = n3.value;
			po.right = n1.value;
			return po;
		} else
			return null;
	}

	private class ParseOperator {
		private String op;
		private Funp left;
		private Funp right;
	}

}
