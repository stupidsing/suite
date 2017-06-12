package suite.funp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import suite.assembler.Amd64;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.assembler.Amd64Assembler;
import suite.funp.Funp_.PN0;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpFixed;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpPolyType;
import suite.funp.P0.FunpVariable;
import suite.funp.P1.FunpFramePointer;
import suite.funp.P1.FunpMemory;
import suite.immutable.IMap;
import suite.node.io.TermOp;
import suite.primitive.Bytes;

/**
 * Hindley-Milner type inference.
 *
 * @author ywsing
 */
public class P1GenerateCode {

	private Amd64 amd64 = new Amd64();
	private OpReg ebp = amd64.reg("EBP");

	private OpReg[] stack = new OpReg[] { //
			amd64.reg("EAX"), //
			amd64.reg("EBX"), //
			amd64.reg("ESI"), };

	private List<Instruction> instructions = new ArrayList<>();

	public Bytes compile(IMap<String, PN0> env, PN0 funp, int offset) {
		Amd64Assembler asm = new Amd64Assembler();
		compileReg_(IMap.empty(), 0, 0, funp);
		return asm.assemble(offset, instructions);
	}

	private void compileReg_(IMap<String, PN0> env, int scope, int sp, PN0 n) {
		ParseOperator po;
		OpReg r0 = stack[sp];

		if (n instanceof FunpApply) {
			compileReg_(env, scope, sp, ((FunpApply) n).value);
			instructions.add(amd64.instruction(Insn.PUSH, r0));
			// TODO
		} else if (n instanceof FunpBoolean) {
			Operand op1 = amd64.imm(((FunpBoolean) n).b ? 1 : 0, Funp_.integerSize);
			instructions.add(amd64.instruction(Insn.MOV, r0, op1));
		} else if (n instanceof FunpFixed) {
		} else if (n instanceof FunpFramePointer) {
			instructions.add(amd64.instruction(Insn.MOV, r0, ebp));
			int scopeLevel1 = ((FunpFramePointer) n).scope;
			while (scope != scopeLevel1) {
				instructions.add(amd64.instruction(Insn.MOV, ebp, amd64.mem(ebp, 0, Funp_.integerSize)));
				scopeLevel1--;
			}
		} else if (n instanceof FunpIf) {
			Operand elseLabel = amd64.imm(0, Funp_.pointerSize);
			Operand endLabel = amd64.imm(0, Funp_.pointerSize);
			compileReg_(env, sp, scope, ((FunpIf) n).if_);
			instructions.add(amd64.instruction(Insn.OR, r0, r0));
			instructions.add(amd64.instruction(Insn.JE, elseLabel));
			compileReg_(env, sp, scope, ((FunpIf) n).then);
			instructions.add(amd64.instruction(Insn.JMP, endLabel));
			instructions.add(amd64.instruction(Insn.LABEL, elseLabel));
			compileReg_(env, sp, scope, ((FunpIf) n).else_);
			instructions.add(amd64.instruction(Insn.LABEL, endLabel));
		} else if (n instanceof FunpLambda) {
			int scope1 = scope + 1;
			PN0 p = null; // new FunpMemory(new FunpFramePointer(scope1), 8, 8);
			IMap<String, PN0> env1 = env.put(((FunpLambda) n).var, p);
			Operand label = amd64.imm(0, Funp_.pointerSize);
			instructions.add(amd64.instruction(Insn.JMP, label));
			instructions.add(amd64.instruction(Insn.PUSH, ebp));
			instructions.add(amd64.instruction(Insn.MOV, ebp, amd64.reg("ESP")));
			compileReg_(env1, 0, scope1, ((FunpLambda) n).expr);
			instructions.add(amd64.instruction(Insn.POP, ebp));
			instructions.add(amd64.instruction(Insn.RET));
			instructions.add(amd64.instruction(Insn.LABEL, label));
		} else if (n instanceof FunpMemory) {
			FunpMemory f1 = (FunpMemory) n;
			int size = f1.end - f1.start;
			if (size <= 4) {
				compileReg_(env, sp, scope, f1.pointer);
				instructions.add(amd64.instruction(Insn.MOV, r0, amd64.mem(r0, f1.start, size)));
			} else
				throw new RuntimeException();
		} else if (n instanceof FunpNumber) {
			Operand op1 = amd64.imm(((FunpNumber) n).i, Funp_.integerSize);
			instructions.add(amd64.instruction(Insn.MOV, r0, op1));
		} else if (n instanceof FunpPolyType)
			compileReg_(env, sp, scope, ((FunpPolyType) n).expr);
		else if ((po = parseOperator(n)) != null && Objects.equals(po.op, TermOp.PLUS__.name)) {
			int sp1 = sp + 1;
			compileReg_(env, sp, scope, po.right);
			compileReg_(env, sp1, scope, po.left);
			instructions.add(amd64.instruction(Insn.ADD, r0, stack[sp1]));
		} else if (n instanceof FunpVariable)
			compileReg_(env, sp, scope, env.get(((FunpVariable) n).var));
		else
			throw new RuntimeException();
	}

	public ParseOperator parseOperator(PN0 f0) {
		FunpApply f1 = f0 instanceof FunpApply ? (FunpApply) f0 : null;
		PN0 f2 = f1 != null ? f1.value : null;
		FunpApply f3 = f2 instanceof FunpApply ? (FunpApply) f2 : null;
		PN0 f4 = f3 != null ? f3.value : null;
		String var = f4 instanceof FunpVariable ? ((FunpVariable) f4).var : null;
		if (var != null) {
			ParseOperator po = new ParseOperator();
			po.op = var;
			po.left = f3.value;
			po.right = f1.value;
			return po;
		} else
			return null;
	}

	private class ParseOperator {
		private String op;
		private PN0 left;
		private PN0 right;
	}

}
