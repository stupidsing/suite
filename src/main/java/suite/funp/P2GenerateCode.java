package suite.funp;

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Fixie_.FixieFun4;
import suite.adt.pair.Pair;
import suite.assembler.Amd64;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpImm;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.assembler.Amd64Assembler;
import suite.assembler.Amd64Parser;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpAsm;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpDontCare;
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
import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.primitive.Bytes;
import suite.primitive.adt.pair.IntIntPair;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Fun2;
import suite.util.FunUtil2.Sink2;
import suite.util.Switch;

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

	Map<TermOp, Insn> jxxInsnByOp = Map.ofEntries( //
			entry(TermOp.EQUAL_, Insn.JE), //
			entry(TermOp.LE____, Insn.JLE), //
			entry(TermOp.LT____, Insn.JL), //
			entry(TermOp.NOTEQ_, Insn.JNE));

	Map<TermOp, Insn> jnxInsnByOp = Map.ofEntries( //
			entry(TermOp.EQUAL_, Insn.JNE), //
			entry(TermOp.LE____, Insn.JG), //
			entry(TermOp.LT____, Insn.JGE), //
			entry(TermOp.NOTEQ_, Insn.JE));

	Map<TermOp, Insn> setInsnByOp = Map.ofEntries( //
			entry(TermOp.EQUAL_, Insn.SETE), //
			entry(TermOp.LE____, Insn.SETLE), //
			entry(TermOp.LT____, Insn.SETL), //
			entry(TermOp.NOTEQ_, Insn.SETNE));

	public List<Instruction> compile0(Funp funp) {
		List<Instruction> instructions = new ArrayList<>();
		P2Emit emit = new P2Emit(instructions::add);
		new Compile0(CompileOut_.OPREG, emit).new Compile1(registerSet, 0).compile(funp);
		return instructions;
	}

	public Bytes compile1(int offset, List<Instruction> instructions, boolean dump) {
		return asm.assemble(offset, instructions, dump);
	}

	private class Compile0 {
		private P2Emit em;
		private CompileOut_ type;
		private FunpMemory target; // only for CompileOutType.ASSIGN
		private OpReg pop0, pop1; // only for CompileOutType.OPSPEC, TWOOPSPEC

		private Compile0(CompileOut_ type, P2Emit emit) {
			this(type, emit, null, null, null);
		}

		private Compile0(CompileOut_ type, P2Emit emit, FunpMemory target, OpReg pop0, OpReg pop1) {
			this.type = type;
			this.em = emit;
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
			private CompileOut compile(Funp n) {
				boolean isOutSpec = type == CompileOut_.OPSPEC || type == CompileOut_.TWOOPSPEC;

				Fun<Operand, CompileOut> postOp = op -> {
					Operand old = op;
					if (type == CompileOut_.ASSIGN)
						em.mov(amd64.mem(mask(op).compileOpReg(target.pointer), target.start, is), old);
					else if (type == CompileOut_.OP || type == CompileOut_.OPREG) {
						if (type == CompileOut_.OPREG && !(op instanceof OpReg))
							em.mov(op = rs.get(), old);
						return new CompileOut(op);
					} else if (type == CompileOut_.OPSPEC)
						em.mov(pop0, old);
					else
						throw new RuntimeException();
					return new CompileOut();
				};

				Fun2<Operand, Operand, CompileOut> postTwoOp = (op0, op1) -> {
					Operand old0 = op0;
					Operand old1 = op1;
					if (type == CompileOut_.ASSIGN) {
						OpReg r = mask(op0, op1).compileOpReg(target.pointer);
						em.mov(amd64.mem(r, target.start, ps), old0);
						em.mov(amd64.mem(r, target.start + ps, ps), old1);
					} else if (type == CompileOut_.TWOOP || type == CompileOut_.TWOOPREG) {
						if (type == CompileOut_.TWOOPREG && !(op0 instanceof OpReg))
							em.mov(op0 = rs.mask(op1).get(), old0);
						if (type == CompileOut_.TWOOPREG && !(op1 instanceof OpReg))
							em.mov(op1 = rs.mask(op0).get(), old1);
						return new CompileOut(op0, op1);
					} else if (type == CompileOut_.TWOOPSPEC) {
						OpReg r = rs.mask(old1, pop1).get(pop0);
						em.mov(r, old0);
						em.mov(pop1, old1);
						em.mov(pop0, r);
					} else
						throw new RuntimeException();
					return new CompileOut();
				};

				Fun<Sink2<Compile1, FunpMemory>, CompileOut> postAssign = assign -> {
					if (type == CompileOut_.ASSIGN) {
						assign.sink2(this, target);
						return new CompileOut();
					} else if (type == CompileOut_.OP || type == CompileOut_.OPREG || type == CompileOut_.OPSPEC) {
						OpReg op0 = isOutSpec ? pop0 : rs.get();
						em.emit(amd64.instruction(Insn.PUSH, eax));
						int fd1 = fd - is;
						assign.sink2(new Compile1(rs, fd1), frame(fd1, fd));
						em.mov(op0, amd64.mem(ebp, fd1, is));
						em.emit(amd64.instruction(Insn.POP, rs.mask(op0).get()));
						return postOp.apply(op0);
					} else if (type == CompileOut_.TWOOP || type == CompileOut_.TWOOPREG || type == CompileOut_.TWOOPSPEC) {
						OpReg op0 = isOutSpec ? pop0 : rs.get();
						OpReg op1 = isOutSpec ? pop1 : rs.mask(op0).get();
						int size = ps * 2;
						int fd1 = fd - size;
						Operand imm = amd64.imm(size);
						em.emit(amd64.instruction(Insn.SUB, esp, imm));
						assign.sink2(new Compile1(rs, fd1), frame(fd1, fd));
						em.mov(op0, amd64.mem(ebp, fd1, ps));
						em.mov(op1, amd64.mem(ebp, fd1 + ps, ps));
						em.emit(amd64.instruction(Insn.ADD, esp, imm));
						return postTwoOp.apply(op0, op1);
					} else
						throw new RuntimeException();
				};

				Fun<Runnable, CompileOut> postRoutine = routine -> compileRoutine(routine).map(postTwoOp);

				return new Switch<CompileOut>(n //
				).applyIf(FunpAllocStack.class, f -> f.apply((size0, value, expr) -> {
					int is1 = is - 1;
					int size1 = (size0 + is1) & ~is1;
					Operand imm = amd64.imm(size1);
					int fd1 = fd - size1;

					if (size1 == is && value != null)
						em.emit(amd64.instruction(Insn.PUSH, compileOp(value)));
					else {
						em.emit(amd64.instruction(Insn.SUB, esp, imm));
						compileAssign(value, frame(fd1, fd1 + size0));
					}
					CompileOut out = new Compile1(rs, fd1).compile(expr);
					if (size1 == is)
						em.emit(amd64.instruction(Insn.POP, rs.mask(out.op0, out.op1).get()));
					else
						em.emit(amd64.instruction(Insn.ADD, esp, imm));
					return out;
				})).applyIf(FunpAsm.class, f -> f.apply((asm, expr) -> {
					Amd64Parser p = new Amd64Parser();
					Read.from(asm).map(p::parse).sink(em::emit);
					return compile(expr);
				})).applyIf(FunpAssign.class, f -> f.apply((memory, value, expr) -> {
					compileAssign(value, memory);
					return compile(expr);
				})).applyIf(FunpBoolean.class, f -> f.apply(b -> {
					return postOp.apply(amd64.imm(b ? 1 : 0, Funp_.booleanSize));
				})).applyIf(FunpData.class, f -> f.apply(pairs -> {
					return postAssign.apply((c1, target) -> {
						for (Pair<Funp, IntIntPair> pair : pairs) {
							IntIntPair offset = pair.t1;
							FunpMemory target_ = FunpMemory.of(target.pointer, target.start + offset.t0, target.end + offset.t1);
							c1.compileAssign(pair.t0, target_);
						}
					});
				})).applyIf(FunpDontCare.class, f -> {
					if (type == CompileOut_.OP || type == CompileOut_.OPREG)
						return new CompileOut(eax);
					else if (type == CompileOut_.TWOOP || type == CompileOut_.TWOOPREG)
						return new CompileOut(eax, edx);
					else
						return new CompileOut();
				}).applyIf(FunpFixed.class, f -> f.apply((var, expr) -> {
					throw new RuntimeException();
				})).applyIf(FunpFramePointer.class, t -> {
					return postOp.apply(ebp);
				}).applyIf(FunpIf.class, f -> f.apply((if_, then, else_) -> {
					OpReg op = isOutSpec ? pop0 : rs.get();
					Operand condLabel = amd64.imm(0, ps);
					Operand endLabel = amd64.imm(0, ps);

					FixieFun4<Insn, Funp, Funp, Operand, Boolean> jmpIf = (insn, lhs, rhs, label) -> {
						OpReg op0 = compileOpReg(lhs);
						Operand op1 = mask(op0).compileOp(rhs);
						em.emit(amd64.instruction(Insn.CMP, op0, op1));
						em.emit(amd64.instruction(insn, label));
						return true;
					};

					class JumpIf {
						private FunpTree tree;
						private Operator operator;
						private Funp left, right;

						private JumpIf(Funp node) {
							tree = node instanceof FunpTree ? (FunpTree) node : null;
							operator = tree != null ? tree.operator : null;
							left = tree != null ? tree.left : null;
							right = tree != null ? tree.right : null;
						}

						private boolean jnxIf(Operand label) {
							Insn jnx = operator != null ? jnxInsnByOp.get(operator) : null;
							if (operator == TermOp.BIGAND)
								return new JumpIf(left).jnxIf(label) && new JumpIf(right).jnxIf(label);
							else if (operator == TermOp.NOTEQ_ && right instanceof FunpBoolean && ((FunpBoolean) right).b)
								return new JumpIf(left).jxxIf(label);
							else if (jnx != null)
								return jmpIf.apply(jnx, left, right, label);
							else
								return false;
						}

						private boolean jxxIf(Operand label) {
							Insn jxx = operator != null ? jxxInsnByOp.get(operator) : null;
							if (operator == TermOp.BIGOR_)
								return new JumpIf(left).jxxIf(label) && new JumpIf(right).jxxIf(label);
							else if (operator == TermOp.NOTEQ_ && right instanceof FunpBoolean && ((FunpBoolean) right).b)
								return new JumpIf(left).jnxIf(label);
							else if (jxx != null)
								return jmpIf.apply(jxx, left, right, label);
							else
								return false;
						}
					}

					Sink2<Funp, Funp> thenElse = (condt, condf) -> {
						compileOpSpec(condt, op);
						em.emit(amd64.instruction(Insn.JMP, endLabel));
						em.emit(amd64.instruction(Insn.LABEL, condLabel));
						compileOpSpec(condf, op);
						em.emit(amd64.instruction(Insn.LABEL, endLabel));
					};

					JumpIf jumpIf = new JumpIf(if_);

					if (jumpIf.jnxIf(condLabel))
						thenElse.sink2(then, else_);
					else if (jumpIf.jxxIf(condLabel))
						thenElse.sink2(else_, then);
					else {
						OpReg r0 = compileOpReg(if_);
						em.emit(amd64.instruction(Insn.OR, r0, r0));
						em.emit(amd64.instruction(Insn.JZ, condLabel));
						thenElse.sink2(then, else_);
					}

					return postOp.apply(op);
				})).applyIf(FunpInvokeInt.class, f -> f.apply(routine -> {
					if (!rs.contains(eax)) {
						compileInvoke(routine);
						return postOp.apply(eax);
					} else
						throw new RuntimeException();
				})).applyIf(FunpInvokeInt2.class, f -> f.apply(routine -> {
					if (!rs.contains(eax, edx)) {
						compileInvoke(routine);
						return postTwoOp.apply(eax, edx);
					} else
						throw new RuntimeException();
				})).applyIf(FunpInvokeIo.class, f -> f.apply(routine -> {
					return postAssign.apply((c1, target) -> {
						OpReg r0 = c1.compileOpReg(target.pointer);
						Compile1 c2 = c1.mask(r0);
						c2.compileInvoke(routine);
						c2.compileMove(r0, target.start, ebp, c1.fd, target.size());
					});
				})).applyIf(FunpMemory.class, f -> f.apply((pointer, start, end) -> {
					int size = end - start;
					if (type == CompileOut_.ASSIGN)
						return postAssign.apply((c1, target) -> {
							OpReg r0 = c1.compileOpReg(target.pointer);
							OpReg r1 = c1.mask(r0).compileOpReg(pointer);
							if (size == target.size())
								c1.mask(r0, r1).compileMove(r0, target.start, r1, start, size);
							else
								throw new RuntimeException();
						});
					else if (type == CompileOut_.OP || type == CompileOut_.OPREG || type == CompileOut_.OPSPEC)
						return postOp.apply(amd64.mem(compileOpReg(pointer), start, size));
					else if (type == CompileOut_.TWOOP || type == CompileOut_.TWOOPREG || type == CompileOut_.TWOOPSPEC) {
						OpReg r = compileOpReg(pointer);
						Operand op0 = amd64.mem(r, start, ps);
						Operand op1 = amd64.mem(r, start + is, ps);
						return postTwoOp.apply(op0, op1);
					} else
						throw new RuntimeException();
				})).applyIf(FunpNumber.class, f -> f.apply(i -> {
					return postOp.apply(amd64.imm(i, is));
				})).applyIf(FunpRoutine.class, f -> f.apply(expr -> {
					return postRoutine.apply(() -> em.mov(eax, new Compile1(registerSet, 0).compileOpReg(expr)));
				})).applyIf(FunpRoutine2.class, f -> f.apply(expr -> {
					return postRoutine.apply(() -> {
						Compile1 c1 = new Compile1(registerSet, 0);
						if (type == CompileOut_.TWOOPSPEC)
							c1.compileTwoOpSpec(expr, pop0, pop1);
						else {
							CompileOut out = c1.compileTwoOp(expr);
							em.mov(eax, out.op0);
							em.mov(edx, out.op1);
						}
					});
				})).applyIf(FunpRoutineIo.class, f -> f.apply((expr, is, os) -> {
					FunpMemory out = frame(ps + is, os);
					return postRoutine.apply(() -> new Compile1(registerSet, 0).compileAssign(expr, out));
				})).applyIf(FunpSaveRegisters.class, f -> f.apply(expr -> {
					OpReg[] opRegs = rs.list(r -> r != esp.reg);

					for (int i = 0; i <= opRegs.length - 1; i++)
						em.emit(amd64.instruction(Insn.PUSH, opRegs[i]));

					CompileOut out0 = new Compile1(registerSet, fd - opRegs.length * is).compile(expr);
					Operand op0 = isOutSpec ? pop0 : out0.op0;
					Operand op1 = isOutSpec ? pop1 : out0.op1;

					if (op0 != null) {
						op0 = rs.contains(op0) ? rs.mask(op1).get() : op0;
						em.mov(op0, out0.op0);
					}

					if (op1 != null) {
						op1 = rs.contains(op1) ? rs.mask(op0).get() : op1;
						em.mov(op1, out0.op1);
					}

					CompileOut out1 = new CompileOut(op0, op1);

					for (int i = opRegs.length - 1; 0 <= i; i--)
						em.emit(amd64.instruction(Insn.POP, opRegs[i]));

					return out1;
				})).applyIf(FunpTree.class, f -> f.apply((operator, lhs, rhs) -> {
					Integer numLhs = lhs instanceof FunpNumber ? ((FunpNumber) lhs).i : null;
					Integer numRhs = rhs instanceof FunpNumber ? ((FunpNumber) rhs).i : null;
					OpMem op = em.decomposeOpMem(n, is);
					Operand op0;

					if (op != null)
						em.lea(op0 = isOutSpec ? pop0 : rs.get(), op);
					else if (numLhs != null && numRhs != null)
						op0 = amd64.imm(TreeUtil.evaluateOp(operator).apply(numLhs, numRhs), is);
					else {
						Source<OpReg> compileLhs = () -> isOutSpec ? compileOpSpec(lhs, pop0) : compileOpReg(lhs);
						Source<OpReg> compileRhs = () -> isOutSpec ? compileOpSpec(rhs, pop0) : compileOpReg(rhs);

						Fun2<OpReg, Sink2<? super OpReg, Integer>, OpReg> fun = (op_, s) -> {
							if (op_ == null && numRhs != null)
								s.sink2(op_ = compileLhs.source(), numRhs);
							if (op_ == null && numLhs != null)
								s.sink2(op_ = compileRhs.source(), numLhs);
							return op_;
						};

						OpReg opResult = null;
						opResult = operator == TermOp.BIGAND ? fun.apply(opResult, em::andImm) : opResult;
						opResult = operator == TermOp.BIGOR_ ? fun.apply(opResult, em::orImm) : opResult;
						opResult = operator == TermOp.PLUS__ ? fun.apply(opResult, em::addImm) : opResult;
						opResult = operator == TermOp.MULT__ ? fun.apply(opResult, em::imulImm) : opResult;

						if (opResult == null && operator == TermOp.MINUS_ && numLhs != null) {
							Instruction instruction = amd64.instruction(Insn.NEG, opResult = compileRhs.source());
							em.emit(instruction);
							em.addImm(opResult, numLhs);
						}

						if (opResult == null && operator == TermOp.MINUS_ && numRhs != null)
							em.addImm(opResult = compileLhs.source(), -numRhs);

						if (opResult == null && operator == TermOp.DIVIDE && numRhs != null && Integer.bitCount(numRhs) == 1) {
							int z = Integer.numberOfTrailingZeros(numRhs);
							opResult = compileRhs.source();
							if (z != 0)
								em.emit(amd64.instruction(Insn.SHR, opResult, amd64.imm(z, 1)));
						}

						Insn setInsn = setInsnByOp.get(operator);

						if (opResult == null)
							if (operator == TermOp.DIVIDE) {
								OpReg opResult_ = isOutSpec ? pop0 : rs.get(eax);
								Sink<Compile1> sink0 = c1 -> {
									c1.compileOpSpec(lhs, eax);
									Operand opRhs0 = c1.mask(eax).compileOp(rhs);
									Operand opRhs1 = !(opRhs0 instanceof OpImm) ? opRhs0 : c1.rs.mask(eax, edx).get();
									em.mov(opRhs1, opRhs0);
									em.emit(amd64.instruction(Insn.XOR, edx, edx));
									em.emit(amd64.instruction(Insn.IDIV, opRhs1));
									em.mov(opResult_, eax);
								};
								Sink<Compile1> sink1 = rs.contains(eax) ? c1 -> c1.saveRegs(sink0, eax) : sink0;
								saveRegs(sink1, edx);
								opResult = opResult_;
							} else if (setInsn != null) {
								OpReg reg8;
								if (isOutSpec)
									reg8 = pop0;
								else {
									int reg = rs.get().reg;
									if (reg < 4) // AL, BL, CL or DL
										reg8 = amd64.reg8[reg];
									else
										throw new RuntimeException();
								}
								OpReg opLhs = compileLhs.source();
								Operand opRhs = mask(opLhs).compileOp(rhs);
								em.emit(amd64.instruction(Insn.CMP, opLhs, opRhs));
								em.emit(amd64.instruction(setInsn, reg8));
								opResult = reg8;
							} else if (operator == TermOp.MINUS_) {
								opResult = compileLhs.source();
								Operand op1 = new Compile1(rs.mask(opResult), fd).compileOp(rhs);
								em.emit(amd64.instruction(Insn.SUB, opResult, op1));
							} else {
								Funp first = operator.getAssoc() == Assoc.RIGHT ? rhs : lhs;
								Funp second = operator.getAssoc() == Assoc.RIGHT ? lhs : rhs;
								opResult = isOutSpec ? compileOpSpec(first, pop0) : compileOpReg(first);
								Operand op1 = mask(opResult).compileOp(second);
								if (operator == TermOp.MULT__ && op1 instanceof OpImm)
									em.emit(amd64.instruction(Insn.IMUL, opResult, opResult, op1));
								else
									em.emit(amd64.instruction(insnByOp.get(operator), opResult, op1));
							}

						op0 = opResult;
					}

					return postOp.apply(op0);
				})).applyIf(FunpTree2.class, f -> f.apply((operator, lhs, rhs) -> {
					Integer numLhs = lhs instanceof FunpNumber ? ((FunpNumber) lhs).i : null;
					Integer numRhs = rhs instanceof FunpNumber ? ((FunpNumber) rhs).i : null;

					OpMem op = em.decomposeOpMem(n, is);
					Operand op0;

					if (op != null)
						em.lea(op0 = isOutSpec ? pop0 : rs.get(), op);
					else if (numLhs != null && numRhs != null)
						op0 = amd64.imm(TreeUtil.evaluateOp(operator).apply(numLhs, numRhs), is);
					else {
						Insn insn;

						if (operator == TreeUtil.SHL)
							insn = Insn.SHL;
						else if (operator == TreeUtil.SHR)
							insn = Insn.SHR;
						else
							throw new RuntimeException();

						Compile1 compile1 = mask(ecx);
						OpReg opResult = isOutSpec ? compile1.compileOpSpec(lhs, pop0) : compile1.compileOpReg(lhs);

						if (numRhs != null)
							em.emit(amd64.instruction(insn, opResult, amd64.imm(numRhs, 1)));
						else
							saveRegs(c1 -> {
								Operand opRhs = c1.mask(opResult).compileOpSpec(rhs, cl);
								em.emit(amd64.instruction(insn, opResult, opRhs));
							}, ecx);

						op0 = opResult;
					}

					return postOp.apply(op0);
				})).nonNullResult();
			}

			private Pair<Operand, Operand> compileRoutine(Runnable runnable) {
				Operand routineLabel = amd64.imm(0, ps);
				Operand endLabel = amd64.imm(0, ps);
				em.emit(amd64.instruction(Insn.JMP, endLabel));
				em.emit(amd64.instruction(Insn.LABEL, routineLabel));
				em.emit(amd64.instruction(Insn.PUSH, ebp));
				em.mov(ebp, esp);
				runnable.run();
				em.emit(amd64.instruction(Insn.POP, ebp));
				em.emit(amd64.instruction(Insn.RET));
				em.emit(amd64.instruction(Insn.LABEL, endLabel));
				return Pair.of(ebp, routineLabel);
			}

			private void compileInvoke(Funp n) {
				CompileOut out = compileTwoOp(n);
				em.mov(ebp, out.op0);
				em.emit(amd64.instruction(Insn.CALL, out.op1));
			}

			private void compileAssign(Funp n, FunpMemory target) {
				new Compile0(CompileOut_.ASSIGN, em, target, null, null).new Compile1(rs, fd).compile(n);
			}

			private Operand compileOp(Funp n) {
				return new Compile0(CompileOut_.OP, em).new Compile1(rs, fd).compile(n).op0;
			}

			private OpReg compileOpReg(Funp n) {
				return (OpReg) new Compile0(CompileOut_.OPREG, em).new Compile1(rs, fd).compile(n).op0;
			}

			private OpReg compileOpSpec(Funp n, OpReg op) {
				new Compile0(CompileOut_.OPSPEC, em, null, op, null).new Compile1(rs, fd).compile(n);
				return op;
			}

			private CompileOut compileTwoOp(Funp n) {
				return new Compile0(CompileOut_.TWOOP, em).new Compile1(rs, fd).compile(n);
			}

			private CompileOut compileTwoOpSpec(Funp n, OpReg op0, OpReg op1) {
				new Compile0(CompileOut_.TWOOPSPEC, em, null, op0, op1).new Compile1(rs, fd).compile(n);
				return new CompileOut(pop0, pop1);
			}

			private void compileMove(OpReg r0, int start0, OpReg r1, int start1, int size) {
				Sink2<Compile1, OpReg> sink = (c1, r) -> {
					int s = r.size;
					em.mov(r, amd64.mem(r1, start1, s));
					em.mov(amd64.mem(r0, start0, s), r);
					c1.compileMove(r0, start0 + s, r1, start1 + s, size - s);
				};

				if (r0 != r1 || start0 != start1)
					if (16 < size)
						saveRegs(c1 -> {
							OpReg r = rs.mask(r0, edi).get(esi);
							em.emit(amd64.instruction(Insn.LEA, r, amd64.mem(r1, start1, is)));
							em.emit(amd64.instruction(Insn.LEA, edi, amd64.mem(r0, start0, is)));
							em.mov(esi, r);
							em.mov(ecx, amd64.imm(size / 4, is));
							em.emit(amd64.instruction(Insn.CLD));
							em.emit(amd64.instruction(Insn.REP));
							em.emit(amd64.instruction(Insn.MOVSD));
							for (int i = 0; i < size % 4; i++)
								em.emit(amd64.instruction(Insn.MOVSB));
						}, ecx, esi, edi);
					else if (is <= size)
						sink.sink2(this, rs.get());
					else if (0 < size)
						saveRegs(c1 -> sink.sink2(c1, cl), ecx);
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
					em.emit(amd64.instruction(Insn.PUSH, op));
					saveRegs(sink, rs_.unmask(op.reg), fd_ - op.size, index + 1, opRegs);
					em.emit(amd64.instruction(Insn.POP, op));
				} else
					sink.sink(new Compile1(rs_, fd_));
			}

			private Compile1 mask(Operand... ops) {
				return new Compile1(rs.mask(ops), fd);
			}
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

}
