package suite.funp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import suite.adt.pair.Pair;
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
import suite.funp.P1.FunpInvokeInt;
import suite.funp.P1.FunpInvokeInt2;
import suite.funp.P1.FunpMemory;
import suite.funp.P1.FunpRoutine;
import suite.funp.P1.FunpRoutine2;
import suite.funp.P1.FunpSaveFramePointer;
import suite.funp.P1.FunpSaveRegisters;
import suite.node.io.Operator;
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

	private Amd64 amd64 = Amd64.me;
	private Amd64Assembler asm = new Amd64Assembler();

	private OpReg cl = amd64.cl;
	private OpReg eax = amd64.eax;
	private OpReg ecx = amd64.ecx;
	private OpReg edx = amd64.edx;
	private OpReg ebp = amd64.ebp;
	private OpReg esp = amd64.esp;
	private OpReg esi = amd64.esi;
	private OpReg edi = amd64.edi;

	private RegisterSet registerSet = new RegisterSet().mask(ebp, esp);

	private Map<Operator, Insn> insnByOp = Read.<Operator, Insn> empty2() //
			.cons(TermOp.BIGOR_, Insn.OR) //
			.cons(TermOp.BIGAND, Insn.AND) //
			.cons(TermOp.PLUS__, Insn.ADD) //
			.cons(TermOp.MINUS_, Insn.SUB) //
			.cons(TermOp.MULT__, Insn.IMUL) //
			.toMap();

	private interface CompileSink<T> {
		public T compile( //
				RegisterSet rs, // register stack pointer
				int fd, // = ESP - EBP
				Funp node);
	}

	public List<Instruction> compile0(Funp funp) {
		Compile0 compile0 = new Compile0();
		compile0.compileReg(registerSet, 0, funp);
		return compile0.instructions;
	}

	public Bytes compile1(int offset, List<Instruction> instructions) {
		return asm.assemble(offset, instructions);
	}

	private class Compile0 {
		private List<Instruction> instructions = new ArrayList<>();

		private Pair<OpReg, OpReg> compileReg2(RegisterSet rs, int fd, Funp n0) {
			Pair<Operand, Operand> pair = compileOp2(rs, fd, n0);
			Operand op0 = pair.t0;
			Operand op1 = pair.t1;
			OpReg r0, r1;
			if (op0 instanceof OpReg)
				r0 = (OpReg) op0;
			else
				emitMov(r0 = rs.get(), op0);
			if (op1 instanceof OpReg)
				r1 = (OpReg) op1;
			else
				emitMov(r1 = rs.mask(r0).get(), op0);
			return Pair.of(r0, r1);
		}

		private OpReg compileReg(RegisterSet rs, int fd, Funp n0) {
			Operand op = compileOp(rs, fd, n0);
			if (op instanceof OpReg)
				return (OpReg) op;
			else {
				OpReg r0 = rs.get();
				emitMov(r0, op);
				return r0;
			}
		}

		private Pair<Operand, Operand> compileOp2(RegisterSet rs, int fd, Funp n0) {
			return compile(rs, fd, this::compileOp2_, n0);
		}

		private Pair<Operand, Operand> compileOp2_(RegisterSet rs, int fd, Funp n0) {
			if (n0 instanceof FunpInvokeInt2) {
				compileInvoke(rs, fd, ((FunpInvokeInt2) n0).routine);
				OpReg r0 = rs.get();
				OpReg r1 = rs.mask(r0).get();
				emitMov(r0, eax);
				emitMov(r1, edx);
				return Pair.of(r0, r1);
			} else if (n0 instanceof FunpMemory) {
				FunpMemory memory = (FunpMemory) n0;
				OpReg r0 = compileReg(rs, fd, memory.range(0, ps));
				OpReg r1 = compileReg(rs.mask(r0), fd, memory.range(ps, ps + ps));
				return Pair.of(r0, r1);
			} else if (n0 instanceof FunpRoutine)
				return compileRoutine(() -> emitMov(eax, compileReg(registerSet, ps, ((FunpRoutine) n0).expr)));
			else if (n0 instanceof FunpRoutine2)
				return compileRoutine(() -> {
					Pair<OpReg, OpReg> pair1 = compileReg2(registerSet, ps, ((FunpRoutine2) n0).expr);
					emitMov(eax, pair1.t0);
					emitMov(edx, pair1.t1);
				});
			else {
				Pair<OpReg, OpReg> pair = compileReg2(rs, fd, n0);
				return Pair.of(pair.t0, pair.t1);
			}
		}

		private Operand compileOp(RegisterSet rs, int fd, Funp n0) {
			return compile(rs, fd, this::compileOp_, n0);
		}

		private Operand compileOp_(RegisterSet rs, int fd, Funp n0) {
			if (n0 instanceof FunpBoolean)
				return amd64.imm(((FunpBoolean) n0).b ? 1 : 0, Funp_.booleanSize);
			else if (n0 instanceof FunpFixed)
				return rs.get(); // TODO
			else if (n0 instanceof FunpFramePointer)
				return ebp;
			else if (n0 instanceof FunpInvokeInt) {
				compileInvoke(rs, fd, ((FunpInvokeInt) n0).routine);
				OpReg r0 = rs.get();
				emitMov(r0, eax);
				return r0;
			} else if (n0 instanceof FunpMemory) {
				FunpMemory n1 = (FunpMemory) n0;
				int size = n1.size();
				if (size == is)
					return amd64.mem(compileReg(rs, fd, n1.pointer), n1.start, size);
				else
					throw new RuntimeException();
			} else if (n0 instanceof FunpNumber)
				return amd64.imm(((FunpNumber) n0).i, is);
			else if (n0 instanceof FunpTree) {
				FunpTree n1 = (FunpTree) n0;
				Operator operator = n1.operator;
				OpReg r0 = compileReg(rs, fd, n1.getFirst());
				OpReg r1 = compileReg(rs.mask(r0), fd, n1.getSecond());
				emit(amd64.instruction(insnByOp.get(operator), r0, r1));
				return r0;
			} else
				throw new RuntimeException("cannot generate code for " + n0);
		}

		private void compileInvoke(RegisterSet rs, int fd, Funp n0) {
			Pair<Operand, Operand> pair = compileOp2(rs, fd, n0);
			emitMov(ebp, pair.t0);
			emit(amd64.instruction(Insn.CALL, pair.t1));
		}

		private Pair<Operand, Operand> compileRoutine(Runnable runnable) {
			Operand routineLabel = amd64.imm(0, ps);
			Operand endLabel = amd64.imm(0, ps);
			emit(amd64.instruction(Insn.JMP, endLabel));
			emit(amd64.instruction(Insn.LABEL, routineLabel));
			emit(amd64.instruction(Insn.PUSH, ebp));
			emitMov(ebp, esp);
			runnable.run();
			emit(amd64.instruction(Insn.POP, ebp));
			emit(amd64.instruction(Insn.RET));
			emit(amd64.instruction(Insn.LABEL, endLabel));
			return Pair.of(ebp, routineLabel);
		}

		private boolean compileAssign(RegisterSet rs, int fd, FunpMemory target, Funp node) {
			return compile(rs, fd, (sp_, fd_, n_) -> compileAssign_(sp_, fd_, target, n_), node);
		}

		private boolean compileAssign_(RegisterSet rs0, int fd, FunpMemory target, Funp n0) {
			int size = target.size();
			OpReg r0 = compileReg(rs0, fd, target.pointer);
			RegisterSet rs1 = rs0.mask(r0);

			if (size <= is) {
				OpReg r1 = compileReg(rs1, fd, n0);
				emitMov(amd64.mem(r0, target.start, size), r1);
			} else if (size == ps + ps) {
				Pair<Operand, Operand> pair = compileOp2(rs1, fd, n0);
				emitMov(amd64.mem(r0, target.start, size), pair.t0);
				emitMov(amd64.mem(r0, target.start + ps, size), pair.t1);
			} else if (n0 instanceof FunpMemory) {
				FunpMemory source = (FunpMemory) n0;
				if (size == source.size()) {
					OpReg r1 = compileReg(rs0, fd, source.pointer);
					compileMove(rs0.mask(r0), r0, target.start, r1, source.start, size);
				} else
					throw new RuntimeException();
			} else
				throw new RuntimeException("cannot assign from " + n0);

			return true;
		}

		private <T> T compile(RegisterSet rs, int fd, CompileSink<T> c, Funp n0) {
			T t;

			if (n0 instanceof FunpAllocStack) {
				FunpAllocStack n1 = (FunpAllocStack) n0;
				int size = n1.size;
				Funp value = n1.value;

				Operand imm = amd64.imm(size);

				if (size == is)
					emit(amd64.instruction(Insn.PUSH, compileOp(rs, fd, value)));
				else {
					emit(amd64.instruction(Insn.SUB, esp, imm));
					compileAssign(rs, fd, FunpMemory.of(new FunpFramePointer(), fd, fd + size), value);
				}
				t = compile(rs, fd - size, c, n1.expr);
				if (size == is)
					emit(amd64.instruction(Insn.POP, rs.get()));
				else
					emit(amd64.instruction(Insn.ADD, esp, imm));
			} else if (n0 instanceof FunpAssign) {
				FunpAssign n1 = (FunpAssign) n0;
				compileAssign(rs, fd, n1.memory, n1.value);
				return compile(rs, fd, c, n1.expr);
			} else if (n0 instanceof FunpIf) {
				Operand elseLabel = amd64.imm(0, ps);
				Operand endLabel = amd64.imm(0, ps);
				OpReg r0 = compileReg(rs, fd, ((FunpIf) n0).if_);
				emit(amd64.instruction(Insn.OR, r0, r0));
				emit(amd64.instruction(Insn.JZ, elseLabel));
				T t0 = compile(rs, fd, c, ((FunpIf) n0).then);
				emit(amd64.instruction(Insn.JMP, endLabel));
				emit(amd64.instruction(Insn.LABEL, elseLabel));
				T t1 = compile(rs, fd, c, ((FunpIf) n0).else_);
				emit(amd64.instruction(Insn.LABEL, endLabel));
				if (Objects.equals(t0, t1))
					t = t0;
				else
					throw new RuntimeException();
			} else if (n0 instanceof FunpSaveFramePointer) {
				emit(amd64.instruction(Insn.PUSH, ebp));
				t = compile(rs, fd - ps, c, ((FunpSaveFramePointer) n0).expr);
				emit(amd64.instruction(Insn.POP, ebp));
				return t;
			} else if (n0 instanceof FunpSaveRegisters) {
				OpReg[] opRegs = rs.list();
				for (int i = 0; i <= opRegs.length - 1; i++)
					emit(amd64.instruction(Insn.PUSH, opRegs[i]));
				t = compile(rs, fd - opRegs.length * is, c, ((FunpSaveRegisters) n0).expr);
				for (int i = opRegs.length - 1; 0 <= i; i--)
					emit(amd64.instruction(Insn.POP, opRegs[i]));
			} else
				t = c.compile(rs, fd, n0);

			return t;
		}

		private void compileMove(RegisterSet rs, OpReg r0, int start0, OpReg r1, int start1, int size) {
			if (size <= 16)
				saveRegs(rs, () -> {
					int i = 0;
					while (i < size) {
						int s = i + is <= size ? is : 1;
						OpReg r = 1 < s ? ecx : cl;
						emitMov(r, amd64.mem(r1, start1 + i, s));
						emitMov(amd64.mem(r0, start0 + i, s), r);
						i += s;
					}
				}, ecx);
			else
				saveRegs(rs, () -> {
					emit(amd64.instruction(Insn.LEA, esi, amd64.mem(r1, start1, is)));
					emit(amd64.instruction(Insn.LEA, edi, amd64.mem(r0, start0, is)));
					emitMov(ecx, amd64.imm(size / 4, 4));
					emit(amd64.instruction(Insn.CLD));
					emit(amd64.instruction(Insn.REP));
					emit(amd64.instruction(Insn.MOVSD));
					for (int i = 0; i < size % 4; i++)
						emit(amd64.instruction(Insn.MOVSB));
					emit(amd64.instruction(Insn.POP, esi));
				}, ecx, esi, edi);
		}

		private void saveRegs(RegisterSet rs, Runnable runnable, OpReg... opRegs) {
			saveRegs(rs, runnable, 0, opRegs);
		}

		private void saveRegs(RegisterSet rs, Runnable runnable, int index, OpReg... opRegs) {
			OpReg op;
			if (index < opRegs.length && rs.isSet(op = opRegs[index])) {
				emit(amd64.instruction(Insn.PUSH, op));
				saveRegs(rs, runnable, index + 1, opRegs);
				emit(amd64.instruction(Insn.POP, op));
			} else
				runnable.run();
		}

		private void emitMov(Operand op0, Operand op1) {
			if (op0 != op1)
				emit(amd64.instruction(Insn.MOV, op0, op1));
		}

		private void emit(Instruction instruction) {
			instructions.add(instruction);
		}
	}

}
