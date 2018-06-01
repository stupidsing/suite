package suite.funp;

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import suite.adt.Mutable;
import suite.adt.pair.Fixie_.FixieFun4;
import suite.adt.pair.Pair;
import suite.assembler.Amd64;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpImm;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.assembler.Amd64Assemble;
import suite.assembler.Amd64Parse;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpAsm;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpCoerce;
import suite.funp.P0.FunpCoerce.Coerce;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpError;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpTree2;
import suite.funp.P2.FunpAllocGlobal;
import suite.funp.P2.FunpAllocReg;
import suite.funp.P2.FunpAllocStack;
import suite.funp.P2.FunpAssign;
import suite.funp.P2.FunpData;
import suite.funp.P2.FunpFramePointer;
import suite.funp.P2.FunpInvoke;
import suite.funp.P2.FunpInvoke2;
import suite.funp.P2.FunpInvokeIo;
import suite.funp.P2.FunpMemory;
import suite.funp.P2.FunpOperand;
import suite.funp.P2.FunpRoutine;
import suite.funp.P2.FunpRoutine2;
import suite.funp.P2.FunpRoutineIo;
import suite.funp.P2.FunpSaveRegisters;
import suite.funp.P2.FunpWhile;
import suite.node.Atom;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.primitive.Bytes;
import suite.streamlet.Read;
import suite.util.Fail;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Fun2;
import suite.util.FunUtil2.Sink2;

public class P4GenerateCode {

	private int is = Funp_.integerSize;
	private int ps = Funp_.pointerSize;

	private Amd64 amd64 = Amd64.me;
	private Amd64Assemble asm = new Amd64Assemble();

	private OpReg cl = amd64.cl;
	private OpReg eax = amd64.eax;
	private OpReg ebx = amd64.ebx;
	private OpReg ecx = amd64.ecx;
	private OpReg edx = amd64.edx;
	private OpReg ebp = amd64.ebp;
	private OpReg esp = amd64.esp;
	private OpReg esi = amd64.esi;
	private OpReg edi = amd64.edi;
	private OpReg[] integerRegs = is == 4 ? amd64.reg32 : is == 8 ? amd64.reg64 : null;
	private RegisterSet registerSet;
	private boolean isUseEbp;

	private OpReg i_eax = eax;
	private OpReg p2_eax = eax;
	private OpReg p2_edx = edx;

	private Map<Object, Insn> insnByOp = Map.ofEntries( //
			entry(TermOp.BIGOR_, Insn.OR), //
			entry(TermOp.BIGAND, Insn.AND), //
			entry(TermOp.PLUS__, Insn.ADD), //
			entry(TermOp.MINUS_, Insn.SUB), //
			entry(TermOp.MULT__, Insn.IMUL), //
			entry(TreeUtil.AND, Insn.AND), //
			entry(TreeUtil.OR_, Insn.OR), //
			entry(TreeUtil.XOR, Insn.XOR));

	private Map<TermOp, Insn> setInsnByOp = Map.ofEntries( //
			entry(TermOp.EQUAL_, Insn.SETE), //
			entry(TermOp.LE____, Insn.SETLE), //
			entry(TermOp.LT____, Insn.SETL), //
			entry(TermOp.NOTEQ_, Insn.SETNE));

	private Map<TermOp, Insn> setRevInsnByOp = Map.ofEntries( //
			entry(TermOp.EQUAL_, Insn.SETE), //
			entry(TermOp.LE____, Insn.SETGE), //
			entry(TermOp.LT____, Insn.SETG), //
			entry(TermOp.NOTEQ_, Insn.SETNE));

	private Map<Atom, Insn> shInsnByOp = Map.ofEntries( //
			entry(TreeUtil.SHL, Insn.SHL), //
			entry(TreeUtil.SHR, Insn.SHR));

	private P4DecomposeOperand deOp;

	public P4GenerateCode(boolean isUseEbp) { // or use ESP directly
		this.isUseEbp = isUseEbp;
		registerSet = new RegisterSet().mask(isUseEbp ? ebp : null, esp);
		deOp = new P4DecomposeOperand(isUseEbp);
	}

	public List<Instruction> compile0(Funp funp) {
		var instructions = new ArrayList<Instruction>();
		var emit = new P4Emit(instructions::add);
		if (isUseEbp)
			emit.mov(ebp, esp);
		new Compile0(Result.OPSPEC4, emit, null, ebx, null).new Compile1(registerSet, 0).compile(funp);
		emit.mov(eax, amd64.imm(1, is));
		emit.emit(amd64.instruction(Insn.INT, amd64.imm(-128)));
		return instructions;
	}

	public Bytes compile1(int offset, List<Instruction> instructions, boolean dump) {
		return asm.assemble(offset, instructions, dump);
	}

	private class Compile0 {
		private P4Emit em;
		private Result result;
		private boolean isOutSpec;
		private FunpMemory target; // only for Result.ASSIGN
		private OpReg pop0, pop1; // only for Result.OPSPEC, TWOOPSPEC

		private Compile0(Result type, P4Emit emit) {
			this(type, emit, null, null, null);
		}

		private Compile0(Result result, P4Emit emit, FunpMemory target, OpReg pop0, OpReg pop1) {
			this.em = emit;
			this.result = result;
			this.isOutSpec = result == Result.OPSPEC4 || result == Result.TWOOPSPEC4;
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
				Fun<Operand, CompileOut> postOp = op -> {
					var old = op;
					if (result == Result.ASSIGN) {
						var opt = deOp.decomposeFunpMemory(fd, target);
						opt = opt != null ? opt : amd64.mem(mask(op).compileOpReg4(target.pointer), target.start, target.size());
						if (op instanceof OpMem)
							em.mov(op = rs.mask(opt).get(old.size), old);
						em.mov(opt, op);
					} else if (result == Result.OP4 || result == Result.OPREG4) {
						if (result == Result.OPREG4 && !(op instanceof OpReg))
							em.mov(op = rs.get(old.size), old);
						return new CompileOut(op);
					} else if (result == Result.OPSPEC4)
						em.mov(pop0, old);
					else
						Fail.t();
					return new CompileOut();
				};

				Fun2<Operand, Operand, CompileOut> postTwoOp = (op0, op1) -> {
					var old0 = op0;
					var old1 = op1;
					if (result == Result.ASSIGN) {
						var opt0 = deOp.decompose(fd, target.pointer, target.start, ps);
						var opt1 = deOp.decompose(fd, target.pointer, target.start + ps, ps);
						if (opt0 == null || opt1 == null) {
							var r = mask(op0, op1).compileOpReg4(target.pointer);
							opt0 = amd64.mem(r, target.start, ps);
							opt1 = amd64.mem(r, target.start + ps, ps);
						}
						if (op0 instanceof OpMem)
							em.mov(op0 = rs.mask(opt0, opt1, op1).get(op0.size), old0);
						em.mov(opt0, op0);
						if (op1 instanceof OpMem)
							em.mov(op1 = rs.mask(opt1).get(op1.size), old1);
						em.mov(opt1, op1);
					} else if (result == Result.TWOOP4 || result == Result.TWOOPREG4) {
						if (result == Result.TWOOPREG4 && !(op0 instanceof OpReg))
							em.mov(op0 = rs.mask(op1).get(old0.size), old0);
						if (result == Result.TWOOPREG4 && !(op1 instanceof OpReg))
							em.mov(op1 = rs.mask(op0).get(old1.size), old1);
						return new CompileOut(op0, op1);
					} else if (result == Result.TWOOPSPEC4) {
						var r = rs.mask(old1, pop1).get(pop0);
						em.mov(r, old0);
						em.mov(pop1, old1);
						em.mov(pop0, r);
					} else
						Fail.t();
					return new CompileOut();
				};

				Fun<Sink2<Compile1, FunpMemory>, CompileOut> postAssign = assign -> {
					if (result == Result.ASSIGN) {
						assign.sink2(this, target);
						return new CompileOut();
					} else if (result == Result.OP4 || result == Result.OPREG4 || result == Result.OPSPEC4) {
						var op0 = isOutSpec ? pop0 : rs.get(is);
						compileAllocStack(is, FunpDontCare.of(), List.of(op0), c1 -> {
							assign.sink2(c1, frame(c1.fd, fd));
							return new CompileOut();
						});
						return postOp.apply(op0);
					} else if (result == Result.TWOOP4 || result == Result.TWOOPREG4 || result == Result.TWOOPSPEC4) {
						var op0 = isOutSpec ? pop0 : rs.get(is);
						var op1 = isOutSpec ? pop1 : rs.mask(op0).get(is);
						compileAllocStack(ps + ps, FunpDontCare.of(), List.of(op1, op0), c1 -> {
							assign.sink2(c1, frame(c1.fd, fd));
							return new CompileOut();
						});
						return postTwoOp.apply(op0, op1);
					} else
						return Fail.t();
				};

				Source<CompileOut> postDontCare = () -> {
					if (result == Result.OP4 || result == Result.OPREG4)
						return new CompileOut(i_eax);
					else if (result == Result.TWOOP4 || result == Result.TWOOPREG4)
						return new CompileOut(p2_eax, p2_edx);
					else
						return new CompileOut();
				};

				Fun<Operand, FixieFun4<Insn, Insn, Funp, Funp, Boolean>> cmpJmp = label -> (insn, revInsn, lhs, rhs) -> {
					var pair = compileCommutativeTree(Insn.CMP, Assoc.RIGHT, lhs, rhs);
					em.emit(amd64.instruction(pair.t0 == lhs ? insn : revInsn, label));
					return true;
				};

				return n.<CompileOut> switch_( //
				).applyIf(FunpAllocGlobal.class, f -> f.apply((var, size, expr, address) -> {
					compileGlobal(size, address);
					return compile(expr);
				})).applyIf(FunpAllocStack.class, f -> f.apply((size, value, expr, offset) -> {
					return compileAllocStack(size, value, null, c -> {
						offset.update(c.fd);
						return c.compile(expr);
					});
				})).applyIf(FunpAllocReg.class, f -> f.apply((size, value, expr, reg) -> {
					var reg_ = rs.get(size);
					reg.update(reg_);
					return mask(compileLoad(value, reg_)).compile(expr);
				})).applyIf(FunpAsm.class, f -> f.apply((assigns, asm) -> {
					var p = new Amd64Parse();
					new Object() {
						private Object assign(Compile1 c1, int i) {
							return i < assigns.size() ? assigns.get(i).map((op, f) -> {
								c1.compileLoad(f, op);
								return assign(c1.mask(op), i + 1);
							}) : this;
						}
					}.assign(this, 0);

					Read.from(asm).map(p::parse).sink(em::emit);
					return postOp.apply(eax);
				})).applyIf(FunpAssign.class, f -> f.apply((memory, value, expr) -> {
					compileAssign(value, memory);
					return compile(expr);
				})).applyIf(FunpBoolean.class, f -> f.apply(b -> {
					return postOp.apply(amd64.imm(b ? 1 : 0, Funp_.booleanSize));
				})).applyIf(FunpCoerce.class, f -> f.apply((coerce, expr) -> {
					if (coerce == Coerce.BYTE) {
						var r1 = pop1 != null && pop1.reg < 4 ? pop1 : rs.get(1);
						var r0 = integerRegs[r1.reg];
						compileOpSpec4(expr, r0);
						return postOp.apply(r1);
					} else
						return compile(expr);
				})).applyIf(FunpData.class, f -> f.apply(pairs -> {
					return postAssign.apply((c1, target) -> {
						for (var pair : pairs) {
							var offset = pair.t1;
							var target_ = FunpMemory.of(target.pointer, target.start + offset.t0, target.start + offset.t1);
							c1.compileAssign(pair.t0, target_);
						}
					});
				})).applyIf(FunpDontCare.class, f -> {
					return postDontCare.source();
				}).applyIf(FunpError.class, f -> {
					em.emit(amd64.instruction(Insn.HLT));
					return postDontCare.source();
				}).applyIf(FunpFramePointer.class, t -> {
					return postOp.apply(compileFramePointer());
				}).applyIf(FunpIf.class, f -> f.apply((if_, then, else_) -> {
					Sink<Funp> compile0, compile1;
					Source<CompileOut> out;

					if (result == Result.ASSIGN || isOutSpec) {
						compile0 = compile1 = this::compile;
						out = CompileOut::new;
					} else if (result == Result.OP4 || result == Result.OPREG4) {
						var ops = new OpReg[1];
						compile0 = node_ -> {
							var op0 = compileOp4(node_);
							em.mov(ops[0] = rs.get(op0), op0);
						};
						compile1 = node_ -> compileOpSpec4(node_, ops[0]);
						out = () -> postOp.apply(ops[0]);
					} else if (result == Result.TWOOP4 || result == Result.TWOOPREG4) {
						var ops = new OpReg[2];
						compile0 = node_ -> {
							var co1 = compileTwoOp4(node_);
							em.mov(ops[0] = rs.mask(co1.op1).get(co1.op0), co1.op0);
							em.mov(ops[1] = rs.mask(ops[0]).get(co1.op1), co1.op1);
						};
						compile1 = node_ -> compileTwoOpSpec4(node_, ops[0], ops[1]);
						out = () -> postTwoOp.apply(ops[0], ops[1]);
					} else
						throw new RuntimeException();

					var condLabel = em.label();
					var endLabel = em.label();

					Sink2<Funp, Funp> thenElse = (condt, condf) -> {
						compile0.sink(condt);
						em.emit(amd64.instruction(Insn.JMP, endLabel));
						em.emit(amd64.instruction(Insn.LABEL, condLabel));
						compile1.sink(condf);
						em.emit(amd64.instruction(Insn.LABEL, endLabel));
					};

					var jumpIf = new P4JumpIf(cmpJmp.apply(condLabel)).new JumpIf(if_);
					Source<Boolean> r;

					if ((r = jumpIf.jnxIf()) != null && r.source())
						thenElse.sink2(then, else_);
					else if ((r = jumpIf.jxxIf()) != null && r.source())
						thenElse.sink2(else_, then);
					else {
						compileJumpZero(if_, condLabel);
						thenElse.sink2(then, else_);
					}

					return out.source();
				})).applyIf(FunpInvoke.class, f -> f.apply(routine -> {
					if (!rs.contains(i_eax)) {
						compileInvoke(routine);
						return postOp.apply(i_eax);
					} else
						return Fail.t();
				})).applyIf(FunpInvoke2.class, f -> f.apply(routine -> {
					if (!rs.contains(p2_eax, p2_edx)) {
						compileInvoke(routine);
						return postTwoOp.apply(p2_eax, p2_edx);
					} else
						return Fail.t();
				})).applyIf(FunpInvokeIo.class, f -> f.apply((routine, is, os) -> {
					compileInvoke(routine);
					return postAssign.apply((c1, target) -> {
						OpReg r0, r1;
						var c2 = c1.mask(r0 = c1.compileOpReg4(target.pointer));
						var c3 = c2.mask(r1 = c2.compileFramePointer());
						c3.compileMove(r0, target.start, r1, c3.fd + is, target.size());
					});
				})).applyIf(FunpMemory.class, f -> f.apply((pointer, start, end) -> {
					var size = end - start;
					Operand op0, op1;
					if (result == Result.ASSIGN)
						if (size == target.size())
							return postAssign.apply((c1, target) -> {
								var op_ = deOp.decomposeFunpMemory(fd, target);
								if (op_ != null)
									c1.compileInstruction(Insn.MOV, op_, n);
								else {
									var r0 = c1.compileOpReg4(target.pointer);
									var r1 = c1.mask(r0).compileOpReg4(pointer);
									c1.mask(r0, r1).compileMove(r0, target.start, r1, start, size);
								}
							});
						else
							return Fail.t();
					else if (result == Result.OP4 || result == Result.OPREG4 || result == Result.OPSPEC4)
						if ((op0 = deOp.decompose(fd, pointer, start, size)) != null)
							return postOp.apply(op0);
						else
							return postOp.apply(amd64.mem(compileOpReg4(pointer), start, size));
					else if (result == Result.TWOOP4 || result == Result.TWOOPREG4 || result == Result.TWOOPSPEC4)
						if ((op0 = deOp.decompose(fd, pointer, start, ps)) != null
								&& (op1 = deOp.decompose(fd, pointer, start + ps, ps)) != null)
							return postTwoOp.apply(op0, op1);
						else {
							var r = compileOpReg4(pointer);
							return postTwoOp.apply(amd64.mem(r, start, ps), amd64.mem(r, start + ps, ps));
						}
					else
						return Fail.t();
				})).applyIf(FunpNumber.class, f -> f.apply(i -> {
					return postOp.apply(amd64.imm(i.get(), is));
				})).applyIf(FunpOperand.class, f -> f.apply(op -> {
					return postOp.apply(op.get());
				})).applyIf(FunpRoutine.class, f -> f.apply((frame, expr) -> {
					return postTwoOp.apply(compileOp4(frame), compileRoutine(c1 -> c1.compileOpSpec4(expr, i_eax)));
				})).applyIf(FunpRoutine2.class, f -> f.apply((frame, expr) -> {
					return postTwoOp.apply(compileOp4(frame), compileRoutine(c1 -> c1.compileTwoOpSpec4(expr, p2_eax, p2_edx)));
				})).applyIf(FunpRoutineIo.class, f -> f.apply((frame, expr, is, os) -> {
					// input argument, return address and EBP
					var o = ps + ps + is;
					var out = frame(o, o + os);
					return postTwoOp.apply(compileOp4(frame), compileRoutine(c1 -> c1.compileAssign(expr, out)));
				})).applyIf(FunpSaveRegisters.class, f -> f.apply(expr -> {
					var opRegs = rs.list(r -> r != ebp.reg && r != esp.reg);

					for (var i = 0; i <= opRegs.length - 1; i++)
						em.emit(amd64.instruction(Insn.PUSH, opRegs[i]));

					var out0 = new Compile1(registerSet, fd - opRegs.length * is).compile(expr);
					Operand oldOp0, oldOp1;
					var op0 = oldOp0 = isOutSpec ? pop0 : out0.op0;
					var op1 = oldOp1 = isOutSpec ? pop1 : out0.op1;

					if (op0 != null)
						em.mov(op0 = rs.contains(op0) ? rs.mask(op1).get(op0.size) : op0, oldOp0);
					if (op1 != null)
						em.mov(op1 = rs.contains(op1) ? rs.mask(op0).get(op1.size) : op1, oldOp1);

					var out1 = new CompileOut(op0, op1);

					for (var i = opRegs.length - 1; 0 <= i; i--)
						em.emit(amd64.instruction(Insn.POP, opRegs[i]));

					return out1;
				})).applyIf(FunpTree.class, f -> f.apply((op, lhs, rhs) -> {
					return postOp.apply(compileTree(n, op, op.assoc(), lhs, rhs));
				})).applyIf(FunpTree2.class, f -> f.apply((op, lhs, rhs) -> {
					return postOp.apply(compileTree(n, op, Assoc.RIGHT, lhs, rhs));
				})).applyIf(FunpWhile.class, f -> f.apply((while_, do_, expr) -> {
					var loopLabel = em.label();
					var contLabel = em.label();
					var exitLabel = em.label();

					em.emit(amd64.instruction(Insn.LABEL, loopLabel));
					Source<Boolean> r;

					if ((r = new P4JumpIf(cmpJmp.apply(exitLabel)).new JumpIf(while_).jnxIf()) != null && r.source())
						;
					else if ((r = new P4JumpIf(cmpJmp.apply(contLabel)).new JumpIf(while_).jxxIf()) != null && r.source()) {
						em.emit(amd64.instruction(Insn.JMP, exitLabel));
						em.emit(amd64.instruction(Insn.LABEL, contLabel));
					} else
						compileJumpZero(while_, exitLabel);

					compileOp4(do_);
					em.emit(amd64.instruction(Insn.JMP, loopLabel));
					em.emit(amd64.instruction(Insn.LABEL, exitLabel));
					return compile(expr);
				})).nonNullResult();
			}

			private CompileOut compileAllocStack(int size, Funp value, List<Operand> opPops, Fun<Compile1, CompileOut> f) {
				var ism1 = is - 1;
				var alignedSize = (size + ism1) & ~ism1;
				var fd1 = fd - alignedSize;
				var c1 = new Compile1(rs, fd1);
				Operand op;

				if (size == is && (op = deOp.decomposeNumber(fd, value)) != null)
					em.emit(amd64.instruction(Insn.PUSH, op));
				else {
					em.addImm(esp, -alignedSize);
					c1.compileAssign(value, FunpMemory.of(Funp_.framePointer, fd1, fd1 + size));
				}
				var out = f.apply(c1);
				if (opPops != null)
					opPops.forEach(opPop -> em.emit(amd64.instruction(Insn.POP, opPop)));
				else if (size == is)
					em.emit(amd64.instruction(Insn.POP, rs.mask(pop0, pop1, out.op0, out.op1).get(size)));
				else
					em.addImm(esp, alignedSize);
				return out;
			}

			private Operand compileTree(Funp n, Object operator, Assoc assoc, Funp lhs, Funp rhs) {
				var numRhs = rhs.cast(FunpNumber.class, n_ -> n_.i.get());
				var insn = insnByOp.get(operator);
				var setInsn = setInsnByOp.get(operator);
				var setRevInsn = setRevInsnByOp.get(operator);
				var shInsn = shInsnByOp.get(operator);
				var op = deOp.decompose(fd, n, 0, is);
				Operand opResult = null;

				if (opResult == null && op != null)
					em.lea(opResult = isOutSpec ? pop0 : rs.get(ps), op);

				if (opResult == null && operator == TermOp.OR____) {
					compileLoad4(lhs);
					opResult = compileOp4(rhs);
				}

				if (opResult == null && operator == TermOp.DIVIDE && numRhs != null && Integer.bitCount(numRhs) == 1)
					em.shiftImm(Insn.SHR, opResult = compileLoad4(rhs), Integer.numberOfTrailingZeros(numRhs));

				if (opResult == null)
					if (operator == TermOp.DIVIDE) {
						var opResult_ = isOutSpec ? pop0 : rs.get(eax);
						Sink<Compile1> sink0 = c1 -> {
							c1.compileOpSpec4(lhs, eax);
							var opRhs0 = c1.mask(eax).compileOp4(rhs);
							var opRhs1 = !(opRhs0 instanceof OpImm) ? opRhs0 : c1.rs.mask(eax, edx).get(is);
							em.mov(opRhs1, opRhs0);
							em.mov(edx, amd64.imm(0l));
							em.emit(amd64.instruction(Insn.IDIV, opRhs1));
							em.mov(opResult_, eax);
						};
						Sink<Compile1> sink1 = rs.contains(eax) ? c1 -> c1.saveRegs(sink0, eax) : sink0;
						saveRegs(sink1, edx);
						opResult = opResult_;
					} else if (operator == TermOp.MINUS_) {
						var pair = compileCommutativeTree(Insn.SUB, assoc, lhs, rhs);
						if ((opResult = pair.t1) == rhs)
							em.emit(amd64.instruction(Insn.NEG, pair.t1));
					} else if (setInsn != null) {
						var pair = compileCommutativeTree(Insn.CMP, assoc, lhs, rhs);
						em.emit(amd64.instruction(pair.t0 == lhs ? setInsn : setRevInsn, opResult = isOutSpec ? pop0 : rs.get(1)));
					} else if (shInsn != null) {
						var op0 = compileLoad4(lhs);
						if (numRhs != null)
							em.emit(amd64.instruction(shInsn, op0, amd64.imm(numRhs, 1)));
						else
							saveRegs(c1 -> {
								var opRhs = c1.mask(op0).compileOpSpec4(rhs, ecx);
								em.emit(amd64.instruction(shInsn, op0, opRhs));
							}, ecx);
						opResult = op0;
					} else
						opResult = compileCommutativeTree(insn, assoc, lhs, rhs).t1;

				return opResult;
			}

			private Pair<Funp, OpReg> compileCommutativeTree(Insn insn, Assoc assoc, Funp lhs, Funp rhs) {
				var opLhs = deOp.decomposeNumber(fd, lhs);
				var opRhs = deOp.decomposeNumber(fd, rhs);
				var opLhsReg = opLhs instanceof OpReg ? (OpReg) opLhs : null;
				var opRhsReg = opRhs instanceof OpReg ? (OpReg) opRhs : null;

				if (opLhsReg != null && !rs.contains(opLhsReg))
					return Pair.of(lhs, compileRegInstruction(insn, opLhsReg, opRhs, lhs));
				else if (opRhsReg != null && !rs.contains(opRhsReg))
					return Pair.of(rhs, compileRegInstruction(insn, opRhsReg, opLhs, rhs));
				else if (opRhs instanceof OpImm)
					if (insn == Insn.CMP && opLhs != null) {
						em.emit(amd64.instruction(insn, opLhs, opRhs));
						return Pair.of(lhs, null);
					} else
						return Pair.of(lhs, em.emitRegInsn(insn, compileLoad4(lhs), opRhs));
				else if (opLhs instanceof OpImm)
					if (insn == Insn.CMP && opRhs != null) {
						em.emit(amd64.instruction(insn, opRhs, opLhs));
						return Pair.of(rhs, null);
					} else
						return Pair.of(rhs, em.emitRegInsn(insn, compileLoad4(rhs), opLhs));
				else if (opLhs != null)
					return Pair.of(rhs, em.emitRegInsn(insn, compileLoad4(rhs), opLhs));
				else if (opRhs != null)
					return Pair.of(lhs, em.emitRegInsn(insn, compileLoad4(lhs), opRhs));
				else {
					var isRightAssoc = assoc == Assoc.RIGHT;
					var first = isRightAssoc ? rhs : lhs;
					var second = isRightAssoc ? lhs : rhs;
					var op0 = compileLoad4(first);
					var op1 = mask(op0).compileOp4(second);
					return Pair.of(first, em.emitRegInsn(insn, op0, op1));
				}
			}

			private Operand compileRoutine(Sink<Compile1> sink) {
				return compileBlock(() -> {
					em.emit(amd64.instruction(Insn.PUSH, ebp));
					if (isUseEbp)
						em.mov(ebp, esp);
					sink.sink(new Compile1(registerSet, 0));
					em.emit(amd64.instruction(Insn.POP, ebp));
					em.emit(amd64.instruction(Insn.RET));
				});
			}

			private void compileGlobal(Integer size, Mutable<Operand> address) {
				address.update(compileBlock(() -> {
					for (var i = 0; i < size; i++)
						em.emit(amd64.instruction(Insn.NOP));
				}));
			}

			private Operand compileBlock(Runnable runnable) {
				var endLabel = em.label();
				var refLabel = em.label();
				em.emit(amd64.instruction(Insn.JMP, endLabel));
				em.emit(amd64.instruction(Insn.LABEL, refLabel));
				runnable.run();
				em.emit(amd64.instruction(Insn.LABEL, endLabel));
				return refLabel;
			}

			private void compileInstruction(Insn insn, Operand op0, Funp f1) {
				var op1 = deOp.decomposeNumber(fd, f1);
				compileInstruction(insn, op0, op1 != null ? op1 : mask(op0).compileOp4(f1));
			}

			private void compileInstruction(Insn insn, Operand op0, Operand op1) {
				if (op0 instanceof OpMem && op1 instanceof OpMem || op0 instanceof OpImm) {
					var oldOp1 = op1;
					em.emit(amd64.instruction(Insn.MOV, op1 = rs.mask(op0).get(op1.size), oldOp1));
				}
				em.emit(amd64.instruction(insn, op0, op1));
			}

			private OpReg compileRegInstruction(Insn insn, OpReg op0, Operand op1, Funp f1) {
				return em.emitRegInsn(insn, op0, op1 != null ? op1 : mask(op0).compileOp4(f1));
			}

			private void compileInvoke(Funp n) {
				var out = compileTwoOp4(n);
				Operand op;
				if (!new RegisterSet().mask(out.op1).contains(ebp))
					op = out.op1;
				else
					em.mov(op = rs.mask(out.op0).get(ps), out.op1);
				em.mov(ebp, out.op0);
				em.emit(amd64.instruction(Insn.CALL, op));
				if (isUseEbp && out.op0 != ebp)
					em.lea(ebp, amd64.mem(esp, -fd, is));
			}

			private void compileJumpZero(Funp if_, Operand label) {
				var op0 = isOutSpec ? pop0 : rs.get(is);
				compileAllocStack(is, FunpNumber.ofNumber(0), List.of(op0), c1 -> {
					var fd1 = c1.fd;
					c1.compileAssign(if_, frame(fd1, fd1 + Funp_.booleanSize));
					return new CompileOut();
				});
				em.emit(amd64.instruction(Insn.OR, op0, op0));
				em.emit(amd64.instruction(Insn.JZ, label));

				// var r0 = compileOpReg(if_);
				// em.emit(amd64.instruction(Insn.OR, r0, r0));
				// em.emit(amd64.instruction(Insn.JZ, label));
			}

			private OpReg compileFramePointer() {
				var op = rs.get(isOutSpec ? pop0 : ebp);
				em.lea(op, compileFrame(0, ps));
				return op;
			}

			private OpReg compileLoad(Funp node, OpReg op) {
				var size = op.size;
				if (size == is)
					compileOpSpec4(node, op);
				else
					compileAllocStack(size, FunpDontCare.of(), null, c1 -> {
						var fd1 = c1.fd;
						c1.compileAssign(node, frame(fd1, fd1 + size));
						em.mov(op, compileFrame(fd1, size));
						return new CompileOut(op);
					});
				return op;
			}

			private OpReg compileLoad4(Funp node) {
				return isOutSpec ? compileOpSpec4(node, pop0) : compileOpReg4(node);
			}

			private OpMem compileFrame(int start, int size) {
				OpMem op = deOp.decompose(fd, Funp_.framePointer, start, size);
				return op != null ? op : Fail.t();
			}

			private void compileAssign(Funp n, FunpMemory target) {
				new Compile0(Result.ASSIGN, em, target, null, null).new Compile1(rs, fd).compile(n);
			}

			private Operand compileOp4(Funp n) {
				return new Compile0(Result.OP4, em).new Compile1(rs, fd).compile(n).op0;
			}

			private OpReg compileOpReg4(Funp n) {
				return (OpReg) new Compile0(Result.OPREG4, em).new Compile1(rs, fd).compile(n).op0;
			}

			private OpReg compileOpSpec4(Funp n, OpReg op) {
				new Compile0(Result.OPSPEC4, em, null, op, null).new Compile1(rs, fd).compile(n);
				return op;
			}

			private CompileOut compileTwoOp4(Funp n) {
				return new Compile0(Result.TWOOP4, em).new Compile1(rs, fd).compile(n);
			}

			private CompileOut compileTwoOpSpec4(Funp n, OpReg op0, OpReg op1) {
				new Compile0(Result.TWOOPSPEC4, em, null, op0, op1).new Compile1(rs, fd).compile(n);
				return new CompileOut(pop0, pop1);
			}

			private void compileMove(OpReg r0, int start0, OpReg r1, int start1, int size) {
				Sink2<Compile1, OpReg> sink = (c1, r) -> {
					var s = r.size;
					em.mov(r, amd64.mem(r1, start1, s));
					em.mov(amd64.mem(r0, start0, s), r);
					c1.compileMove(r0, start0 + s, r1, start1 + s, size - s);
				};

				if (r0 != r1 || start0 != start1)
					if (16 < size)
						saveRegs(c1 -> {
							var r = rs.mask(r0, edi).get(esi);
							em.lea(r, amd64.mem(r1, start1, is));
							em.lea(edi, amd64.mem(r0, start0, is));
							em.mov(esi, r);
							em.mov(ecx, amd64.imm(size / 4, is));
							em.emit(amd64.instruction(Insn.CLD));
							em.emit(amd64.instruction(Insn.REP));
							em.emit(amd64.instruction(Insn.MOVSD));
							for (var i = 0; i < size % 4; i++)
								em.emit(amd64.instruction(Insn.MOVSB));
						}, ecx, esi, edi);
					else if (is <= size)
						sink.sink2(this, rs.get(is));
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

	private enum Result {
		ASSIGN, // assign value to certain memory region
		OP4, // put value to an operand (r/m or immediate)
		OPREG4, // put value to a register operand
		OPSPEC4, // put value to a specified operand
		TWOOP4, // put value to an operand pair
		TWOOPREG4, // put value to a register operand pair
		TWOOPSPEC4, // put value to a specified operand pair
	};

}
