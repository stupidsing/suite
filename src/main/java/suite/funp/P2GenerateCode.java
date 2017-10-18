package suite.funp;

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.assembler.Amd64;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpImm;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.assembler.Amd64Assembler;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpFixed;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpTree2;
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
import suite.node.Atom;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.primitive.Bytes;
import suite.primitive.IntPrimitives.IntObjSink;
import suite.primitive.adt.pair.IntIntPair;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Fun2;
import suite.util.FunUtil2.Sink2;

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
		new Compile0(CompileOut_.OPREG, instructions::add).new Compile1(registerSet, 0).compile(funp);
		return instructions;
	}

	public Bytes compile1(int offset, List<Instruction> instructions, boolean dump) {
		return asm.assemble(offset, instructions, dump);
	}

	private class Compile0 {
		private Sink<Instruction> emit;
		private CompileOut_ type;
		private FunpMemory target; // only for CompileOutType.ASSIGN
		private OpReg pop0, pop1; // only for CompileOutType.OPSPEC, TWOOPSPEC

		private Compile0(CompileOut_ type, Sink<Instruction> emit) {
			this(type, emit, null, null, null);
		}

		private Compile0(CompileOut_ type, Sink<Instruction> emit, FunpMemory target, OpReg pop0, OpReg pop1) {
			this.type = type;
			this.emit = emit;
			this.target = target;
			this.pop0 = pop0;
			this.pop1 = pop1;
		}

		private class Compile1 {
			private RegisterSet rs;
			private int fd;

			private Compile1(RegisterSet rs, int fd) {
				this.rs = rs;
				this.fd = fd;
			}

			// invariant: fd = ESP - EBP
			private CompileOut compile(Funp n0) {
				boolean isOutSpec = type == CompileOut_.OPSPEC || type == CompileOut_.TWOOPSPEC;

				Fun<Operand, CompileOut> postOp = op -> {
					Operand old = op;
					if (type == CompileOut_.ASSIGN)
						emitMov(amd64.mem(mask(op).compileOpReg(target.pointer), target.start, is), old);
					else if (type == CompileOut_.OP || type == CompileOut_.OPREG) {
						if (type == CompileOut_.OPREG && !(op instanceof OpReg))
							emitMov(op = rs.get(), old);
						return new CompileOut(op);
					} else if (type == CompileOut_.OPSPEC)
						emitMov(pop0, old);
					else
						throw new RuntimeException();
					return new CompileOut();
				};

				Fun2<Operand, Operand, CompileOut> postTwoOp = (op0, op1) -> {
					Operand old0 = op0;
					Operand old1 = op1;
					if (type == CompileOut_.ASSIGN) {
						OpReg r = mask(op0, op1).compileOpReg(target.pointer);
						emitMov(amd64.mem(r, target.start, ps), old0);
						emitMov(amd64.mem(r, target.start + ps, ps), old1);
					} else if (type == CompileOut_.TWOOP || type == CompileOut_.TWOOPREG) {
						if (type == CompileOut_.TWOOPREG && !(op0 instanceof OpReg))
							emitMov(op0 = rs.mask(op1).get(), old0);
						if (type == CompileOut_.TWOOPREG && !(op1 instanceof OpReg))
							emitMov(op1 = rs.mask(op0).get(), old1);
						return new CompileOut(op0, op1);
					} else if (type == CompileOut_.TWOOPSPEC) {
						OpReg r = rs.mask(old1, pop1).get(pop0);
						emitMov(r, old0);
						emitMov(pop1, old1);
						emitMov(pop0, r);
					} else
						throw new RuntimeException();
					return new CompileOut();
				};

				Fun<IntObjSink<FunpMemory>, CompileOut> postAssign = assign -> {
					if (type == CompileOut_.ASSIGN) {
						assign.sink2(fd, target);
						return new CompileOut();
					} else if (type == CompileOut_.OP || type == CompileOut_.OPREG || type == CompileOut_.OPSPEC) {
						OpReg op0 = isOutSpec ? pop0 : rs.get();
						emit(amd64.instruction(Insn.PUSH, eax));
						int fd1 = fd - is;
						assign.sink2(fd1, frame(fd1, fd));
						emitMov(op0, amd64.mem(ebp, fd1, is));
						emit(amd64.instruction(Insn.POP, rs.mask(op0).get()));
						return postOp.apply(op0);
					} else if (type == CompileOut_.TWOOP || type == CompileOut_.TWOOPREG || type == CompileOut_.TWOOPSPEC) {
						OpReg op0 = isOutSpec ? pop0 : rs.get();
						OpReg op1 = isOutSpec ? pop1 : rs.get(op0);
						int size = ps * 2;
						int fd1 = fd - size;
						Operand imm = amd64.imm(size);
						emit(amd64.instruction(Insn.SUB, esp, imm));
						assign.sink2(fd1, frame(fd1, fd));
						emitMov(op0, amd64.mem(ebp, fd1, ps));
						emitMov(op1, amd64.mem(ebp, fd1 + ps, ps));
						emit(amd64.instruction(Insn.ADD, esp, imm));
						return postTwoOp.apply(op0, op1);
					} else
						throw new RuntimeException();
				};

				Fun<Runnable, CompileOut> postRoutine = routine -> compileRoutine(routine).map(postTwoOp);

				if (n0 instanceof FunpAllocStack) {
					FunpAllocStack n1 = (FunpAllocStack) n0;
					int size = n1.size;
					Funp value = n1.value;
					Operand imm = amd64.imm(size);

					if (size == is && value != null)
						emit(amd64.instruction(Insn.PUSH, compileOp(value)));
					else {
						emit(amd64.instruction(Insn.SUB, esp, imm));
						if (value != null)
							compileAssign(value, frame(fd - size, fd));
					}
					CompileOut out = new Compile1(rs, fd - size).compile(n1.expr);
					if (size == is)
						emit(amd64.instruction(Insn.POP, rs.mask(out.op0, out.op1).get()));
					else
						emit(amd64.instruction(Insn.ADD, esp, imm));
					return out;
				} else if (n0 instanceof FunpAssign) {
					FunpAssign n1 = (FunpAssign) n0;
					compileAssign(n1.value, n1.memory);
					return compile(n1.expr);
				} else if (n0 instanceof FunpBoolean)
					return postOp.apply(amd64.imm(((FunpBoolean) n0).b ? 1 : 0, Funp_.booleanSize));
				else if (n0 instanceof FunpData) {
					FunpData n1 = (FunpData) n0;
					List<Funp> data = n1.data;
					IntIntPair[] offsets = n1.offsets;
					return postAssign.apply((fd1, target) -> {
						for (int i = 0; i < data.size(); i++) {
							IntIntPair offset = offsets[i];
							FunpMemory target_ = FunpMemory.of(target.pointer, target.start + offset.t0, target.end + offset.t1);
							new Compile1(rs, fd1).compileAssign(data.get(i), target_);
						}
					});
				} else if (n0 instanceof FunpFixed)
					throw new RuntimeException();
				else if (n0 instanceof FunpFramePointer)
					return postOp.apply(ebp);
				else if (n0 instanceof FunpIf) {
					FunpIf n1 = (FunpIf) n0;
					OpReg op = isOutSpec ? pop0 : rs.get();
					Operand elseLabel = amd64.imm(0, ps);
					Operand endLabel = amd64.imm(0, ps);
					OpReg r0 = compileOpReg(n1.if_);
					emit(amd64.instruction(Insn.OR, r0, r0));
					emit(amd64.instruction(Insn.JZ, elseLabel));
					compileOpSpec(n1.then, op);
					emit(amd64.instruction(Insn.JMP, endLabel));
					emit(amd64.instruction(Insn.LABEL, elseLabel));
					compileOpSpec(n1.else_, op);
					emit(amd64.instruction(Insn.LABEL, endLabel));
					return postOp.apply(op);
				} else if (n0 instanceof FunpInvokeInt)
					if (!rs.contains(eax)) {
						compileInvoke(((FunpInvokeInt) n0).routine);
						return postOp.apply(eax);
					} else
						throw new RuntimeException();
				else if (n0 instanceof FunpInvokeInt2)
					if (!rs.contains(eax, edx)) {
						compileInvoke(((FunpInvokeInt2) n0).routine);
						return postTwoOp.apply(eax, edx);
					} else
						throw new RuntimeException();
				else if (n0 instanceof FunpInvokeIo)
					return postAssign.apply((fd1, target) -> {
						FunpInvokeIo n1 = (FunpInvokeIo) n0;
						Compile1 c1 = new Compile1(rs, fd1);
						OpReg r0 = c1.compileOpReg(target.pointer);
						Compile1 c2 = c1.mask(r0);
						c2.compileInvoke(n1.routine);
						c2.compileMove(r0, target.start, ebp, fd1, target.size());
					});
				else if (n0 instanceof FunpMemory) {
					FunpMemory n1 = (FunpMemory) n0;
					int size = n1.size();
					if (type == CompileOut_.ASSIGN)
						return postAssign.apply((fd1, target) -> {
							Compile1 c1 = new Compile1(rs, fd1);
							OpReg r0 = c1.compileOpReg(target.pointer);
							OpReg r1 = c1.mask(r0).compileOpReg(n1.pointer);
							if (size == target.size())
								c1.mask(r0, r1).compileMove(r0, target.start, r1, n1.start, size);
							else
								throw new RuntimeException();
						});
					else if (type == CompileOut_.OP || type == CompileOut_.OPREG || type == CompileOut_.OPSPEC)
						return postOp.apply(amd64.mem(compileOpReg(n1.pointer), n1.start, size));
					else if (type == CompileOut_.TWOOP || type == CompileOut_.TWOOPREG || type == CompileOut_.TWOOPSPEC) {
						OpReg r = compileOpReg(n1.pointer);
						Operand op0 = amd64.mem(r, n1.start, ps);
						Operand op1 = amd64.mem(r, n1.start + is, ps);
						return postTwoOp.apply(op0, op1);
					} else
						throw new RuntimeException();
				} else if (n0 instanceof FunpNumber)
					return postOp.apply(amd64.imm(((FunpNumber) n0).i, is));
				else if (n0 instanceof FunpRoutine)
					return postRoutine
							.apply(() -> emitMov(eax, new Compile1(registerSet, ps).compileOpReg(((FunpRoutine) n0).expr)));
				else if (n0 instanceof FunpRoutine2)
					if (type == CompileOut_.TWOOPSPEC)
						return postRoutine
								.apply(() -> new Compile1(registerSet, ps).compileTwoOpSpec(((FunpRoutine2) n0).expr, pop0, pop1));
					else
						return postRoutine.apply(() -> {
							CompileOut out = new Compile1(registerSet, ps).compileTwoOp(((FunpRoutine2) n0).expr);
							emitMov(eax, out.op0);
							emitMov(edx, out.op1);
						});
				else if (n0 instanceof FunpRoutineIo) {
					FunpRoutineIo n1 = (FunpRoutineIo) n0;
					FunpMemory out = frame(ps + n1.is, n1.os);
					return postRoutine.apply(() -> new Compile1(registerSet, ps).compileAssign(n1.expr, out));
				} else if (n0 instanceof FunpSaveRegisters) {
					OpReg[] opRegs = rs.list(r -> r != esp.reg);
					for (int i = 0; i <= opRegs.length - 1; i++)
						emit(amd64.instruction(Insn.PUSH, opRegs[i]));
					CompileOut out0 = new Compile1(registerSet, fd - opRegs.length * is).compile(((FunpSaveRegisters) n0).expr);
					Operand op0 = isOutSpec ? pop0 : out0.op0;
					Operand op1 = isOutSpec ? pop1 : out0.op1;

					if (op0 != null) {
						op0 = rs.contains(op0) ? rs.mask(op1).get() : op0;
						emitMov(op0, out0.op0);
					}

					if (op1 != null) {
						op1 = rs.contains(op1) ? rs.mask(op0).get() : op1;
						emitMov(op1, out0.op1);
					}

					CompileOut out1 = new CompileOut(op0, op1);
					for (int i = opRegs.length - 1; 0 <= i; i--)
						emit(amd64.instruction(Insn.POP, opRegs[i]));
					return out1;
				} else if (n0 instanceof FunpTree) {
					FunpTree n1 = (FunpTree) n0;
					Operator operator = n1.operator;
					Funp lhs = n1.left;
					Funp rhs = n1.right;
					Integer numLhs = lhs instanceof FunpNumber ? ((FunpNumber) lhs).i : null;
					Integer numRhs = rhs instanceof FunpNumber ? ((FunpNumber) rhs).i : null;

					OpMem op = decomposeOpMem(n0, is);
					Operand op0, op1;

					if (op != null) {
						op0 = isOutSpec ? pop0 : rs.get();
						if (op.baseReg < 0 && op.indexReg < 0)
							emit(amd64.instruction(Insn.MOV, op0, amd64.imm(op.disp, is)));
						else
							emit(amd64.instruction(Insn.LEA, op0, op));
					} else if (numLhs != null && numRhs != null)
						op0 = amd64.imm(TreeUtil.evaluateOp(operator).apply(numLhs, numRhs), is);
					else {
						Source<OpReg> compileLhs = () -> isOutSpec ? compileOpSpec(lhs, pop0) : compileOpReg(lhs);
						Source<OpReg> compileRhs = () -> isOutSpec ? compileOpSpec(rhs, pop0) : compileOpReg(rhs);

						Fun2<OpReg, Sink2<? super OpReg, Integer>, OpReg> fun = (op_, f) -> {
							if (op_ == null && numRhs != null)
								f.sink2(op_ = compileLhs.source(), numRhs);
							if (op_ == null && numLhs != null)
								f.sink2(op_ = compileRhs.source(), numLhs);
							return op_;
						};

						OpReg opResult = null;
						Compile0 compile0 = Compile0.this;
						opResult = operator == TermOp.BIGAND ? fun.apply(opResult, compile0::emitAndImm) : opResult;
						opResult = operator == TermOp.BIGOR_ ? fun.apply(opResult, compile0::emitOrImm) : opResult;
						opResult = operator == TermOp.PLUS__ ? fun.apply(opResult, compile0::emitAddImm) : opResult;
						opResult = operator == TermOp.MULT__ ? fun.apply(opResult, compile0::emitImulImm) : opResult;

						if (opResult == null && operator == TermOp.MINUS_ && numLhs != null) {
							emit(amd64.instruction(Insn.NEG, opResult = compileRhs.source()));
							emitAddImm(opResult, numLhs);
						}

						if (opResult == null && operator == TermOp.MINUS_ && numRhs != null)
							emitAddImm(opResult = compileLhs.source(), -numRhs);

						if (opResult == null && operator == TermOp.DIVIDE && numRhs != null && Integer.bitCount(numRhs) == 1) {
							int z = Integer.numberOfTrailingZeros(numRhs);
							opResult = compileRhs.source();
							if (z != 0)
								emit(amd64.instruction(Insn.SHR, opResult, amd64.imm(z, 1)));
						}

						if (opResult == null)
							if (operator == TermOp.DIVIDE) {
								OpReg opResult_ = isOutSpec ? pop0 : rs.get(eax);
								Sink<Compile1> sink0 = c1 -> {
									c1.compileOpSpec(lhs, eax);
									Operand opRight = c1.mask(eax).compileOp(rhs);
									emit(amd64.instruction(Insn.XOR, edx, edx));
									emit(amd64.instruction(Insn.IDIV, opRight));
									emitMov(opResult_, eax);
								};
								Sink<Compile1> sink1 = rs.contains(eax) ? c1 -> c1.saveRegs(sink0, eax) : sink0;
								saveRegs(sink1, edx);
								opResult = opResult_;
							} else if (operator == TermOp.MINUS_) {
								opResult = compileLhs.source();
								op1 = new Compile1(rs.mask(opResult), fd).compileOp(lhs);
								emit(amd64.instruction(Insn.SUB, opResult, op1));
							} else {
								Funp first = n1.getFirst();
								opResult = isOutSpec ? compileOpSpec(first, pop0) : compileOpReg(first);
								op1 = mask(opResult).compileOp(n1.getSecond());
								if (operator == TermOp.MULT__ && op1 instanceof OpImm)
									emit(amd64.instruction(Insn.IMUL, opResult, opResult, op1));
								else
									emit(amd64.instruction(insnByOp.get(operator), opResult, op1));
							}

						op0 = opResult;
					}

					return postOp.apply(op0);
				} else if (n0 instanceof FunpTree2) {
					FunpTree2 n1 = (FunpTree2) n0;
					Atom operator = n1.operator;
					Funp lhs = n1.left;
					Funp rhs = n1.right;
					Integer numLhs = lhs instanceof FunpNumber ? ((FunpNumber) lhs).i : null;
					Integer numRhs = rhs instanceof FunpNumber ? ((FunpNumber) rhs).i : null;

					OpMem op = decomposeOpMem(n0, is);
					Operand op0;

					if (op != null) {
						op0 = isOutSpec ? pop0 : rs.get();
						if (op.baseReg < 0 && op.indexReg < 0)
							emit(amd64.instruction(Insn.MOV, op0, amd64.imm(op.disp, is)));
						else
							emit(amd64.instruction(Insn.LEA, op0, op));
					} else if (numLhs != null && numRhs != null)
						op0 = amd64.imm(TreeUtil.evaluateOp(operator).apply(numLhs, numRhs), is);
					else {
						Funp left = n1.left;
						Funp right = n1.right;
						Insn insn;

						if (operator == TreeUtil.SHL)
							insn = Insn.SHL;
						else if (operator == TreeUtil.SHR)
							insn = Insn.SHR;
						else
							throw new RuntimeException();

						Compile1 compile1 = mask(ecx);
						OpReg opResult = isOutSpec ? compile1.compileOpSpec(left, pop0) : compile1.compileOpReg(left);

						if (numRhs != null)
							emit(amd64.instruction(insn, opResult, amd64.imm(numRhs, 1)));
						else
							saveRegs(c1 -> {
								Operand opRhs = c1.mask(opResult).compileOpSpec(right, cl);
								emit(amd64.instruction(insn, opResult, opRhs));
							}, ecx);

						op0 = opResult;
					}

					return postOp.apply(op0);
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

			private void compileInvoke(Funp n0) {
				CompileOut out = compileTwoOp(n0);
				emitMov(ebp, out.op0);
				emit(amd64.instruction(Insn.CALL, out.op1));
			}

			private void compileAssign(Funp n, FunpMemory target) {
				new Compile0(CompileOut_.ASSIGN, emit, target, null, null).new Compile1(rs, fd).compile(n);
			}

			private Operand compileOp(Funp n) {
				return new Compile0(CompileOut_.OP, emit).new Compile1(rs, fd).compile(n).op0;
			}

			private OpReg compileOpReg(Funp n) {
				return (OpReg) new Compile0(CompileOut_.OPREG, emit).new Compile1(rs, fd).compile(n).op0;
			}

			private OpReg compileOpSpec(Funp n, OpReg op) {
				new Compile0(CompileOut_.OPSPEC, emit, null, op, null).new Compile1(rs, fd).compile(n);
				return op;
			}

			private CompileOut compileTwoOp(Funp n) {
				return new Compile0(CompileOut_.TWOOP, emit).new Compile1(rs, fd).compile(n);
			}

			private CompileOut compileTwoOpSpec(Funp n, OpReg op0, OpReg op1) {
				new Compile0(CompileOut_.TWOOPSPEC, emit, null, op0, op1).new Compile1(rs, fd).compile(n);
				return new CompileOut(pop0, pop1);
			}

			private void compileMove(OpReg r0, int start0, OpReg r1, int start1, int size) {
				if (r0 != r1 || start0 != start1)
					if (size <= 16)
						saveRegs(fd_ -> {
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
						saveRegs(fd_ -> {
							OpReg r = rs.mask(r0, edi).get(esi);
							emit(amd64.instruction(Insn.LEA, r, amd64.mem(r1, start1, is)));
							emit(amd64.instruction(Insn.LEA, edi, amd64.mem(r0, start0, is)));
							emitMov(esi, r);
							emitMov(ecx, amd64.imm(size / 4, 4));
							emit(amd64.instruction(Insn.CLD));
							emit(amd64.instruction(Insn.REP));
							emit(amd64.instruction(Insn.MOVSD));
							for (int i = 0; i < size % 4; i++)
								emit(amd64.instruction(Insn.MOVSB));
						}, ecx, esi, edi);
			}

			private OpMem decomposeOpMem(Funp n0, int size) {
				OpReg baseReg = null, indexReg = null;
				int scale = 1, disp = 0;
				boolean ok = is1248(size);

				for (Funp n1 : FunpTree.unfold(n0, TermOp.PLUS__)) {
					DecomposeMult dec = new DecomposeMult(n1);
					if (dec.mults.isEmpty()) {
						OpReg reg_ = dec.reg;
						long scale_ = dec.scale;
						if (reg_ != null)
							if (is1248(scale_) && indexReg == null) {
								indexReg = reg_;
								scale = (int) scale_;
							} else if (scale_ == 1 && baseReg == null)
								baseReg = reg_;
							else
								ok = false;
						else if (reg_ == null)
							disp += scale_;
					} else
						ok = false;
				}

				return ok ? amd64.mem(indexReg, baseReg, scale, disp, size) : null;
			}

			private class DecomposeMult {
				private long scale = 1;
				private OpReg reg;
				private List<Funp> mults = new ArrayList<>();

				private DecomposeMult(Funp n0) {
					decompose(n0);
				}

				private void decompose(Funp n0) {
					FunpTree2 tree;
					Funp r;
					for (Funp n1 : FunpTree.unfold(n0, TermOp.MULT__))
						if (n1 instanceof FunpFramePointer && reg == null)
							reg = ebp;
						else if (n1 instanceof FunpTree2 //
								&& (tree = (FunpTree2) n1).operator == TreeUtil.SHL //
								&& (r = tree.right) instanceof FunpNumber) {
							decompose(tree.left);
							scale <<= 1 << ((FunpNumber) r).i;
						} else if (n1 instanceof FunpNumber)
							scale *= ((FunpNumber) n1).i;
						else
							mults.add(n1);
				}
			}

			private FunpMemory frame(int start, int end) {
				return FunpMemory.of(Funp_.framePointer, start, end);
			}

			private void saveRegs(Sink<Compile1> sink, OpReg... opRegs) {
				saveRegs(sink, rs, fd, 0, opRegs);
			}

			private void saveRegs(Sink<Compile1> sink, RegisterSet rs_, int fd_, int index, OpReg... opRegs) {
				OpReg op;
				if (index < opRegs.length && rs_.contains(op = opRegs[index])) {
					emit(amd64.instruction(Insn.PUSH, op));
					saveRegs(sink, rs_.unmask(op.reg), fd_ - op.size, index + 1, opRegs);
					emit(amd64.instruction(Insn.POP, op));
				} else
					sink.sink(new Compile1(rs_, fd_));
			}

			private Compile1 mask(Operand... ops) {
				return new Compile1(rs.mask(ops), fd);
			}
		}

		private void emitAddImm(Operand op0, int i) {
			if (i == -1)
				emit(amd64.instruction(Insn.DEC, op0));
			else if (i == 1)
				emit(amd64.instruction(Insn.INC, op0));
			else if (i != 0)
				emit(amd64.instruction(Insn.ADD, op0, amd64.imm(i, is)));
		}

		private void emitAndImm(Operand op0, int i) {
			if (i != -1)
				emit(amd64.instruction(Insn.AND, op0, amd64.imm(i, is)));
		}

		private void emitOrImm(Operand op0, int i) {
			if (i != 0)
				emit(amd64.instruction(Insn.AND, op0, amd64.imm(i, is)));
		}

		private void emitImulImm(OpReg r0, int i) {
			if (Integer.bitCount(i) == 1)
				emit(amd64.instruction(Insn.SHL, r0, amd64.imm(Integer.numberOfTrailingZeros(i), 1)));
			else if (i != 1)
				emit(amd64.instruction(Insn.IMUL, r0, r0, amd64.imm(i, is)));
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

	private enum CompileOut_ {
		ASSIGN, // assign value to certain memory region
		OP, // put value to an operand (r/m or immediate)
		OPREG, // put value to a register operand
		OPSPEC, // put value to a specified operand
		TWOOP, // put value to an operand pair
		TWOOPREG, // put value to a register operand pair
		TWOOPSPEC, // put value to a specified operand pair
	};

	private boolean is1248(long scale_) {
		return scale_ == 1 || scale_ == 2 || scale_ == 4 || scale_ == 8;
	}

}
