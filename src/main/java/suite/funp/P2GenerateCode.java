package suite.funp;

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import suite.adt.pair.Pair;
import suite.assembler.Amd64;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.assembler.Amd64Assembler;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpDeref;
import suite.funp.P0.FunpFixed;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpTree;
import suite.funp.P1.FunpAllocStack;
import suite.funp.P1.FunpAssign;
import suite.funp.P1.FunpData;
import suite.funp.P1.FunpFramePointer;
import suite.funp.P1.FunpInvokeInt;
import suite.funp.P1.FunpInvokeInt2;
import suite.funp.P1.FunpInvokeIo;
import suite.funp.P1.FunpMemory;
import suite.funp.P1.FunpRoutine;
import suite.funp.P1.FunpRoutine2;
import suite.funp.P1.FunpRoutineIo;
import suite.funp.P1.FunpSaveRegisters;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.primitive.Bytes;
import suite.primitive.adt.pair.IntIntPair;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil2.Fun2;

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

	private Map<Operator, Insn> insnByOp = Map.ofEntries( //
			entry(TermOp.BIGOR_, Insn.OR), //
			entry(TermOp.BIGAND, Insn.AND), //
			entry(TermOp.PLUS__, Insn.ADD), //
			entry(TermOp.MINUS_, Insn.SUB), //
			entry(TermOp.MULT__, Insn.IMUL));

	public List<Instruction> compile0(Funp funp) {
		List<Instruction> instructions = new ArrayList<>();
		Compile0 compile0 = new Compile0(CompileOutType.OPREG, instructions::add);
		compile0.compile(registerSet, 0, funp);
		return instructions;
	}

	public Bytes compile1(int offset, List<Instruction> instructions, boolean dump) {
		return asm.assemble(offset, instructions, dump);
	}

	// invariant: fd = ESP - EBP
	private class Compile0 {
		private Sink<Instruction> emit;
		private CompileOutType type;
		private FunpMemory target;

		private Compile0(CompileOutType type, Sink<Instruction> emit) {
			this(type, emit, null);
		}

		private Compile0(CompileOutType type, Sink<Instruction> emit, FunpMemory target) {
			this.type = type;
			this.emit = emit;
			this.target = target;
		}

		private CompileOut compile(RegisterSet rs, int fd, Funp n0) {
			Fun<Operand, CompileOut> postOp = op -> {
				Operand old = op;
				if (type == CompileOutType.ASSIGN) {
					emitMov(amd64.mem(compileReg(rs.mask(op), fd, target.pointer), target.start, is), old);
					return new CompileOut();
				} else if (type == CompileOutType.OP || type == CompileOutType.OPREG) {
					if (type == CompileOutType.OPREG && !(op instanceof OpReg))
						emitMov(op = rs.get(), old);
					return new CompileOut(op);
				} else
					throw new RuntimeException();
			};

			Fun2<Operand, Operand, CompileOut> postOp2 = (op0, op1) -> {
				Operand old0 = op0;
				Operand old1 = op1;
				if (type == CompileOutType.ASSIGN) {
					OpReg r = compileReg(rs.mask(op0, op1), fd, target.pointer);
					emitMov(amd64.mem(r, target.start, ps), old0);
					emitMov(amd64.mem(r, target.start + ps, ps), old1);
					return new CompileOut();
				} else if (type == CompileOutType.TWOOP || type == CompileOutType.TWOOPREG) {
					if (type == CompileOutType.TWOOPREG && !(op0 instanceof OpReg))
						emitMov(op0 = rs.mask(op1).get(), old0);
					if (type == CompileOutType.TWOOPREG && !(op1 instanceof OpReg))
						emitMov(op1 = rs.mask(op0).get(), old1);
					return new CompileOut(op0, op1);
				} else
					throw new RuntimeException();
			};

			Fun<Sink<FunpMemory>, CompileOut> postAssign = assign -> {
				if (type == CompileOutType.ASSIGN) {
					assign.sink(target);
					return new CompileOut();
				} else if (type == CompileOutType.OP || type == CompileOutType.OPREG) {
					emit(amd64.instruction(Insn.PUSH, eax));
					assign.sink(FunpMemory.of(new FunpFramePointer(), fd - is, fd));
					CompileOut out = postOp.apply(amd64.mem(ebp, fd - is, is));
					emit(amd64.instruction(Insn.POP, rs.mask(out.op0, out.op1).get()));
					return out;
				} else if (type == CompileOutType.TWOOP || type == CompileOutType.TWOOPREG) {
					int size = ps * 2;
					int fd1 = fd - size;
					Operand imm = amd64.imm(size);
					emit(amd64.instruction(Insn.SUB, esp, imm));
					assign.sink(FunpMemory.of(new FunpFramePointer(), fd1, fd));
					CompileOut out = postOp2.apply(amd64.mem(ebp, fd1, ps), amd64.mem(ebp, fd1 + ps, ps));
					emit(amd64.instruction(Insn.ADD, esp, imm));
					return out;
				} else
					throw new RuntimeException();
			};

			Fun<Runnable, CompileOut> postRoutine = routine -> compileRoutine(routine).map(postOp2);

			if (n0 instanceof FunpAllocStack) {
				FunpAllocStack n1 = (FunpAllocStack) n0;
				int size = n1.size;
				Funp value = n1.value;
				Operand imm = amd64.imm(size);

				if (size == is && value != null)
					emit(amd64.instruction(Insn.PUSH, compileOp(rs, fd, value)));
				else {
					emit(amd64.instruction(Insn.SUB, esp, imm));
					if (value != null)
						compileAssignment(rs, fd, FunpMemory.of(new FunpFramePointer(), fd - size, fd), value);
				}
				CompileOut out = compile(rs, fd - size, n1.expr);
				if (size == is)
					emit(amd64.instruction(Insn.POP, rs.mask(out.op0, out.op1).get()));
				else
					emit(amd64.instruction(Insn.ADD, esp, imm));
				return out;
			} else if (n0 instanceof FunpAssign) {
				FunpAssign n1 = (FunpAssign) n0;
				new Compile0(CompileOutType.ASSIGN, emit, n1.memory).compile(rs, fd, n1.value);
				return compile(rs, fd, n1.expr);
			} else if (n0 instanceof FunpBoolean)
				return postOp.apply(amd64.imm(((FunpBoolean) n0).b ? 1 : 0, Funp_.booleanSize));
			else if (n0 instanceof FunpData) {
				FunpData n1 = (FunpData) n0;
				List<Funp> data = n1.data;
				IntIntPair[] offsets = n1.offsets;
				return postAssign.apply(target -> {
					for (int i = 0; i < data.size(); i++) {
						IntIntPair offset = offsets[i];
						FunpMemory target_ = FunpMemory.of(target.pointer, target.start + offset.t0, target.end + offset.t1);
						compileAssignment(rs, fd, target_, data.get(i));
					}
				});
			} else if (n0 instanceof FunpDeref) {
				FunpDeref n1 = (FunpDeref) n0;
				if (type == CompileOutType.OP || type == CompileOutType.OPREG)
					return postOp.apply(amd64.mem(compileReg(rs, fd, n1.pointer), 0, is));
				else if (type == CompileOutType.TWOOP || type == CompileOutType.TWOOPREG) {
					OpReg r = compileReg(rs, fd, n1.pointer);
					Operand op0 = amd64.mem(r, 0, ps);
					Operand op1 = amd64.mem(r, ps, ps);
					return postOp2.apply(op0, op1);
				} else if (type == CompileOutType.ASSIGN)
					return postAssign.apply(target -> {
						OpReg r0 = compileReg(rs, fd, target.pointer);
						OpReg r1 = compileReg(rs.mask(r0), fd, n1.pointer);
						compileMove(rs.mask(r0, r1), r0, target.start, r1, 0, target.size());
					});
				else
					throw new RuntimeException();
			} else if (n0 instanceof FunpFixed)
				throw new RuntimeException();
			else if (n0 instanceof FunpFramePointer)
				return postOp.apply(ebp);
			else if (n0 instanceof FunpIf)
				if (type == CompileOutType.OP || type == CompileOutType.OPREG) {
					FunpIf n1 = (FunpIf) n0;
					Operand elseLabel = amd64.imm(0, ps);
					Operand endLabel = amd64.imm(0, ps);
					OpReg r0 = compileReg(rs, fd, n1.if_);
					emit(amd64.instruction(Insn.OR, r0, r0));
					emit(amd64.instruction(Insn.JZ, elseLabel));
					Operand t0 = compileOp(rs, fd, n1.then);
					emit(amd64.instruction(Insn.JMP, endLabel));
					emit(amd64.instruction(Insn.LABEL, elseLabel));
					Operand t1 = compileOp(rs, fd, n1.else_);
					emit(amd64.instruction(Insn.LABEL, endLabel));
					if (Objects.equals(t0, t1))
						return postOp.apply(t0);
					else
						throw new RuntimeException();
				} else
					throw new RuntimeException();
			else if (n0 instanceof FunpInvokeInt)
				if (!rs.contains(eax)) {
					compileInvoke(rs, fd, ((FunpInvokeInt) n0).routine);
					return postOp.apply(eax);
				} else
					throw new RuntimeException();
			else if (n0 instanceof FunpInvokeInt2)
				if (!rs.contains(eax, edx)) {
					compileInvoke(rs, fd, ((FunpInvokeInt2) n0).routine);
					return postOp2.apply(eax, edx);
				} else
					throw new RuntimeException();
			else if (n0 instanceof FunpInvokeIo)
				return postAssign.apply(target -> {
					FunpInvokeIo n1 = (FunpInvokeIo) n0;
					OpReg r0 = compileReg(rs, fd, target.pointer);
					RegisterSet rs1 = rs.mask(r0);
					compileInvoke(rs1, fd, n1.routine);
					compileMove(rs1, r0, target.start, ebp, fd, target.size());
				});
			else if (n0 instanceof FunpMemory) {
				FunpMemory n1 = (FunpMemory) n0;
				int size = n1.size();
				if (type == CompileOutType.ASSIGN)
					return postAssign.apply(target -> {
						OpReg r0 = compileReg(rs, fd, target.pointer);
						OpReg r1 = compileReg(rs.mask(r0), fd, n1.pointer);
						if (size == target.size())
							compileMove(rs.mask(r0, r1), r0, target.start, r1, n1.start, size);
						else
							throw new RuntimeException();
					});
				else if (type == CompileOutType.OP || type == CompileOutType.OPREG)
					return postOp.apply(amd64.mem(compileReg(rs, fd, n1.pointer), n1.start, size));
				else if (type == CompileOutType.TWOOP) {
					OpReg r = compileReg(rs, fd, n1.pointer);
					Operand op0 = amd64.mem(r, n1.start, ps);
					Operand op1 = amd64.mem(r, n1.start + is, ps);
					return postOp2.apply(op0, op1);
				} else if (type == CompileOutType.TWOOPREG) {
					Operand op0 = compileReg(rs, fd, n1.range(0, ps));
					Operand op1 = compileReg(rs.mask(op0), fd, n1.range(ps, ps + ps));
					return postOp2.apply(op0, op1);
				} else
					throw new RuntimeException();
			} else if (n0 instanceof FunpNumber)
				return postOp.apply(amd64.imm(((FunpNumber) n0).i, is));
			else if (n0 instanceof FunpRoutine)
				return postRoutine.apply(() -> emitMov(eax, compileReg(registerSet, ps, ((FunpRoutine) n0).expr)));
			else if (n0 instanceof FunpRoutine2)
				return postRoutine.apply(() -> {
					Pair<Operand, Operand> pair1 = compileOp2(registerSet, ps, ((FunpRoutine2) n0).expr);
					emitMov(eax, pair1.t0);
					emitMov(edx, pair1.t1);
				});
			else if (n0 instanceof FunpRoutineIo) {
				FunpRoutineIo n1 = (FunpRoutineIo) n0;
				FunpMemory out = FunpMemory.of(new FunpFramePointer(), ps + n1.is, n1.os);
				return postRoutine.apply(() -> compileAssignment(registerSet, ps, out, n1.expr));
			} else if (n0 instanceof FunpSaveRegisters) {
				OpReg[] opRegs = rs.list(r -> r != esp.reg);
				for (int i = 0; i <= opRegs.length - 1; i++)
					emit(amd64.instruction(Insn.PUSH, opRegs[i]));
				CompileOut out0 = compile(registerSet, fd - opRegs.length * is, ((FunpSaveRegisters) n0).expr);
				Operand op0 = out0.op0;
				Operand op1 = out0.op1;

				if (op0 != null && rs.contains(op0))
					emitMov(op0 = rs.mask(op1).get(), out0.op0);
				if (op1 != null && rs.contains(op1))
					emitMov(op1 = rs.mask(op0).get(), out0.op1);
				CompileOut out1 = new CompileOut(op0, op1);
				for (int i = opRegs.length - 1; 0 <= i; i--)
					emit(amd64.instruction(Insn.POP, opRegs[i]));
				return out1;
			} else if (n0 instanceof FunpTree) {
				OpMem op = decomposeOpMem(n0, is);
				OpReg r0, r1;
				if (op != null)
					emit(amd64.instruction(Insn.LEA, r0 = rs.get(), op));
				else {
					FunpTree n1 = (FunpTree) n0;
					Operator operator = n1.operator;
					r0 = compileReg(rs, fd, n1.getFirst());
					r1 = compileReg(rs.mask(r0), fd, n1.getSecond());
					emit(amd64.instruction(insnByOp.get(operator), r0, r1));
				}
				return postOp.apply(r0);
			} else
				throw new RuntimeException("cannot compile " + n0);
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

		private void compileInvoke(RegisterSet rs, int fd, Funp n0) {
			Pair<Operand, Operand> pair = compileOp2(rs, fd, n0);
			emitMov(ebp, pair.t0);
			emit(amd64.instruction(Insn.CALL, pair.t1));
		}

		private void compileAssignment(RegisterSet rs, int fd, FunpMemory target, Funp n) {
			new Compile0(CompileOutType.ASSIGN, emit, target).compile(rs, fd, n);
		}

		private Operand compileOp(RegisterSet rs, int fd, Funp n) {
			return new Compile0(CompileOutType.OP, emit).compile(rs, fd, n).op0;
		}

		private OpReg compileReg(RegisterSet rs, int fd, Funp n) {
			return (OpReg) new Compile0(CompileOutType.OPREG, emit).compile(rs, fd, n).op0;
		}

		private Pair<Operand, Operand> compileOp2(RegisterSet rs, int fd, Funp n) {
			CompileOut out = new Compile0(CompileOutType.TWOOP, emit).compile(rs, fd, n);
			return Pair.of(out.op0, out.op1);
		}

		private void compileMove(RegisterSet rs, OpReg r0, int start0, OpReg r1, int start1, int size) {
			if (r0 != r1 || start0 != start1)
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

		private OpMem decomposeOpMem(Funp n0, int size) {
			OpReg baseReg = null, indexReg = null;
			int scale = 1, disp = 0;
			boolean ok = is1248(size);

			for (Funp n1 : unfold(n0, TermOp.PLUS__)) {
				DecomposeMult dec = new DecomposeMult(n1);
				if (dec.mults.isEmpty()) {
					OpReg reg_ = dec.reg;
					long scale_ = dec.scale;
					if (reg_ != null) {
						if (is1248(scale_) && indexReg == null) {
							indexReg = reg_;
							scale = (int) scale_;
						} else if (scale_ == 1 && baseReg == null)
							baseReg = reg_;
						else
							ok = false;
					} else if (reg_ == null)
						disp += scale_;
				} else
					ok = false;
			}

			return ok ? amd64.mem(indexReg, baseReg, scale, disp, size) : null;
		}

		private boolean is1248(long scale_) {
			return scale_ == 1 || scale_ == 2 || scale_ == 4 || scale_ == 8;
		}

		private class DecomposeMult {
			private long scale = 1;
			private OpReg reg;
			private List<Funp> mults = new ArrayList<>();

			private DecomposeMult(Funp n0) {
				for (Funp n1 : unfold(n0, TermOp.MULT__))
					if (n1 instanceof FunpFramePointer && reg == null)
						reg = ebp;
					else if (n1 instanceof FunpNumber)
						scale *= ((FunpNumber) n1).i;
					else
						mults.add(n1);
			}
		}

		private List<Funp> unfold(Funp n, Operator op) {
			List<Funp> list = new ArrayList<>();
			FunpTree tree;
			while (n instanceof FunpTree && (tree = (FunpTree) n).operator == op) {
				list.add(tree.left);
				n = tree.right;
			}
			list.add(n);
			return list;
		}

		private void saveRegs(RegisterSet rs, Runnable runnable, OpReg... opRegs) {
			saveRegs(rs, runnable, 0, opRegs);
		}

		private void saveRegs(RegisterSet rs, Runnable runnable, int index, OpReg... opRegs) {
			OpReg op;
			if (index < opRegs.length && rs.contains(op = opRegs[index])) {
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
			emit.sink(instruction);
		}
	}

	private class CompileOut {
		private Operand op0, op1;

		private CompileOut() {
			this(null);
		}

		private CompileOut(Operand op0) {
			this(op0, null);
		}

		private CompileOut(Operand op0, Operand op1) {
			this.op0 = op0;
			this.op1 = op1;
		}
	}

	private enum CompileOutType {
		ASSIGN, OP, OPREG, TWOOP, TWOOPREG,
	};

}
