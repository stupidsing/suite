package suite.funp;

import static java.util.Map.entry;

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
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpCoerce;
import suite.funp.P0.FunpCoerce.Coerce;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpError;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpIoAsm;
import suite.funp.P0.FunpIoWhile;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpTree2;
import suite.funp.P2.FunpAllocGlobal;
import suite.funp.P2.FunpAllocReg;
import suite.funp.P2.FunpAllocStack;
import suite.funp.P2.FunpAssignMem;
import suite.funp.P2.FunpAssignOp;
import suite.funp.P2.FunpCmp;
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
import suite.node.Atom;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.primitive.Bytes;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.FunUtil2.Sink2;
import suite.streamlet.Read;
import suite.util.Fail;

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
		return P4Emit.generate(emit -> {
			if (isUseEbp)
				emit.mov(ebp, esp);
			emit.emit(amd64.instruction(Insn.CLD));
			new Compile0(Result.ISSPEC, emit, null, ebx, null).new Compile1(registerSet, 0).compile(funp);
			emit.mov(eax, amd64.imm(1, is));
			emit.emit(amd64.instruction(Insn.INT, amd64.imm(-128)));
		});
	}

	public Bytes compile1(int offset, List<Instruction> instructions, boolean dump) {
		return asm.assemble(offset, instructions, dump);
	}

	private class Compile0 {
		private P4Emit em;
		private Result result;
		private boolean isOutSpec;
		private FunpMemory target; // only for Result.ASSIGN
		private OpReg pop0, pop1; // only for Result.ISSPEC, PS2SPEC

		private Compile0(Result type, P4Emit emit) {
			this(type, emit, null, null, null);
		}

		private Compile0(Result result, P4Emit emit, FunpMemory target, OpReg pop0, OpReg pop1) {
			this.em = emit;
			this.result = result;
			this.isOutSpec = result == Result.ISSPEC || result == Result.PS2SPEC;
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
				})).applyIf(FunpAssignMem.class, f -> f.apply((target, value, expr) -> {
					compileAssign(value, target);
					return compile(expr);
				})).applyIf(FunpAssignOp.class, f -> f.apply((target, value, expr) -> {
					compileIsSpec(value, (OpReg) target.operand.get());
					return compile(expr);
				})).applyIf(FunpBoolean.class, f -> f.apply(b -> {
					return returnIsOp(amd64.imm(b ? 1 : 0, Funp_.booleanSize));
				})).applyIf(FunpCmp.class, f -> f.apply((op, l, r) -> {
					var isEq = op == TermOp.EQUAL_;
					var r0 = compileIsReg(l.pointer);
					var r1 = mask(r0).compileIsReg(r.pointer);
					var size0 = l.size();
					var size1 = r.size();
					return size0 == size1 ? returnIsOp(compileCompare(r0, l.start, r1, r.start, size0, isEq)) : Fail.t();
				})).applyIf(FunpCoerce.class, f -> f.apply((coerce, expr) -> {
					if (coerce == Coerce.BYTE) {
						var r1 = pop1 != null && pop1.reg < 4 ? pop1 : rs.get(1);
						var r0 = integerRegs[r1.reg];
						compileIsSpec(expr, r0);
						return returnIsOp(r1);
					} else
						return compile(expr);
				})).applyIf(FunpData.class, f -> f.apply(pairs -> {
					return returnAssign((c1, t) -> Read //
							.from2(pairs) //
							.sink((n_, ofs) -> c1.compileAssign(n_, FunpMemory.of(t.pointer, t.start + ofs.t0, t.start + ofs.t1))));
				})).applyIf(FunpDontCare.class, f -> {
					return returnDontCare();
				}).applyIf(FunpError.class, f -> {
					em.emit(amd64.instruction(Insn.HLT));
					return returnDontCare();
				}).applyIf(FunpFramePointer.class, t -> {
					return returnIsOp(compileFramePointer());
				}).applyIf(FunpIf.class, f -> f.apply((if_, then, else_) -> {
					Sink<Funp> compile0, compile1;
					Source<CompileOut> out;

					if (result == Result.ASSIGN || isOutSpec) {
						compile0 = compile1 = this::compile;
						out = CompileOut::new;
					} else if (result == Result.ISOP || result == Result.ISREG) {
						var ops = new OpReg[1];
						compile0 = node_ -> {
							var op0 = compileIsOp(node_);
							ops[0] = em.mov(rs.get(op0), op0);
						};
						compile1 = node_ -> compileIsSpec(node_, ops[0]);
						out = () -> returnIsOp(ops[0]);
					} else if (result == Result.PS2OP || result == Result.PS2REG) {
						var ops = new OpReg[2];
						compile0 = node_ -> {
							var co1 = compilePs2Op(node_);
							ops[0] = em.mov(rs.mask(co1.op1).get(co1.op0), co1.op0);
							ops[1] = em.mov(rs.mask(ops[0]).get(co1.op1), co1.op1);
						};
						compile1 = node_ -> compilePs2Spec(node_, ops[0], ops[1]);
						out = () -> returnPs2Op(ops[0], ops[1]);
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

					var jumpIf = new P4JumpIf(compileCmpJmp(condLabel)).new JumpIf(if_);
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
						return returnIsOp(i_eax);
					} else
						return Fail.t();
				})).applyIf(FunpInvoke2.class, f -> f.apply(routine -> {
					if (!rs.contains(p2_eax, p2_edx)) {
						compileInvoke(routine);
						return returnPs2Op(p2_eax, p2_edx);
					} else
						return Fail.t();
				})).applyIf(FunpInvokeIo.class, f -> f.apply((routine, is, os) -> {
					compileInvoke(routine);
					return returnAssign((c1, target) -> {
						OpReg r0, r1;
						var c2 = c1.mask(r0 = c1.compileIsReg(target.pointer));
						var c3 = c2.mask(r1 = c2.compileFramePointer());
						c3.compileMove(r0, target.start, r1, c3.fd + is, target.size());
					});
				})).applyIf(FunpIoAsm.class, f -> f.apply((assigns, asm) -> {
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
					return returnIsOp(eax);
				})).applyIf(FunpIoWhile.class, f -> f.apply((while_, do_, expr) -> {
					var loopLabel = em.label();
					var contLabel = em.label();
					var exitLabel = em.label();

					em.emit(amd64.instruction(Insn.LABEL, loopLabel));
					Source<Boolean> r;

					if ((r = new P4JumpIf(compileCmpJmp(exitLabel)).new JumpIf(while_).jnxIf()) != null && r.source())
						;
					else if ((r = new P4JumpIf(compileCmpJmp(contLabel)).new JumpIf(while_).jxxIf()) != null && r.source()) {
						em.emit(amd64.instruction(Insn.JMP, exitLabel));
						em.emit(amd64.instruction(Insn.LABEL, contLabel));
					} else
						compileJumpZero(while_, exitLabel);

					compileIsOp(do_);
					em.emit(amd64.instruction(Insn.JMP, loopLabel));
					em.emit(amd64.instruction(Insn.LABEL, exitLabel));
					return compile(expr);
				})).applyIf(FunpMemory.class, f -> f.apply((pointer, start, end) -> {
					var size = end - start;
					Operand op0, op1;
					if (result == Result.ASSIGN)
						if (size == target.size())
							return returnAssign((c1, target) -> {
								var op_ = deOp.decomposeFunpMemory(fd, target);
								if (op_ != null)
									c1.compileInstruction(Insn.MOV, op_, n);
								else {
									var r0 = c1.compileIsReg(target.pointer);
									var r1 = c1.mask(r0).compileIsReg(pointer);
									c1.mask(r0, r1).compileMove(r0, target.start, r1, start, size);
								}
							});
						else
							return Fail.t();
					else if (result == Result.ISOP || result == Result.ISREG || result == Result.ISSPEC)
						if ((op0 = deOp.decompose(fd, pointer, start, size)) != null)
							return returnIsOp(op0);
						else
							return returnIsOp(amd64.mem(compileIsReg(pointer), start, size));
					else if (result == Result.PS2OP || result == Result.PS2REG || result == Result.PS2SPEC)
						if ((op0 = deOp.decompose(fd, pointer, start, ps)) != null
								&& (op1 = deOp.decompose(fd, pointer, start + ps, ps)) != null)
							return returnPs2Op(op0, op1);
						else {
							var r = compileIsReg(pointer);
							return returnPs2Op(amd64.mem(r, start, ps), amd64.mem(r, start + ps, ps));
						}
					else
						return Fail.t();
				})).applyIf(FunpNumber.class, f -> f.apply(i -> {
					return returnIsOp(amd64.imm(i.get(), is));
				})).applyIf(FunpOperand.class, f -> f.apply(op -> {
					return returnIsOp(op.get());
				})).applyIf(FunpRoutine.class, f -> f.apply((frame, expr) -> {
					return returnPs2Op(compileIsOp(frame), compileRoutine(c1 -> c1.compileIsSpec(expr, i_eax)));
				})).applyIf(FunpRoutine2.class, f -> f.apply((frame, expr) -> {
					return returnPs2Op(compileIsOp(frame), compileRoutine(c1 -> c1.compilePs2Spec(expr, p2_eax, p2_edx)));
				})).applyIf(FunpRoutineIo.class, f -> f.apply((frame, expr, is, os) -> {
					// input argument, return address and EBP
					var o = ps + ps + is;
					var out = frame(o, o + os);
					return returnPs2Op(compileIsOp(frame), compileRoutine(c1 -> c1.compileAssign(expr, out)));
				})).applyIf(FunpSaveRegisters.class, f -> f.apply(expr -> {
					var opRegs = rs.list(r -> !registerSet.isSet(r));

					for (var i = 0; i <= opRegs.length - 1; i++)
						em.emit(amd64.instruction(Insn.PUSH, opRegs[i]));

					var out = new Compile1(registerSet, fd - opRegs.length * is).compile(expr);
					var op0 = isOutSpec ? pop0 : out.op0;
					var op1 = isOutSpec ? pop1 : out.op1;

					if (op0 != null)
						op0 = em.mov(rs.contains(op0) ? rs.mask(op1).get(op0.size) : op0, op0);
					if (op1 != null)
						op1 = em.mov(rs.contains(op1) ? rs.mask(op0).get(op1.size) : op1, op1);

					for (var i = opRegs.length - 1; 0 <= i; i--)
						em.emit(amd64.instruction(Insn.POP, opRegs[i]));

					if (op0 != null && isOutSpec)
						op0 = em.mov(pop0, op0);
					if (op1 != null && isOutSpec)
						op1 = em.mov(pop1, op1);

					return new CompileOut(op0, op1);
				})).applyIf(FunpTree.class, f -> f.apply((op, lhs, rhs) -> {
					return returnIsOp(compileTree(n, op, op.assoc(), lhs, rhs));
				})).applyIf(FunpTree2.class, f -> f.apply((op, lhs, rhs) -> {
					return returnIsOp(compileTree(n, op, Assoc.RIGHT, lhs, rhs));
				})).nonNullResult();
			}

			private CompileOut returnAssign(Sink2<Compile1, FunpMemory> assign) {
				if (result == Result.ASSIGN) {
					assign.sink2(this, target);
					return new CompileOut();
				} else if (result == Result.ISOP || result == Result.ISREG || result == Result.ISSPEC) {
					var op0 = isOutSpec ? pop0 : rs.get(is);
					compileAllocStack(is, FunpDontCare.of(), List.of(op0), c1 -> {
						assign.sink2(c1, frame(c1.fd, fd));
						return new CompileOut();
					});
					return returnIsOp(op0);
				} else if (result == Result.PS2OP || result == Result.PS2REG || result == Result.PS2SPEC) {
					var op0 = isOutSpec ? pop0 : rs.get(is);
					var op1 = isOutSpec ? pop1 : rs.mask(op0).get(is);
					compileAllocStack(ps + ps, FunpDontCare.of(), List.of(op1, op0), c1 -> {
						assign.sink2(c1, frame(c1.fd, fd));
						return new CompileOut();
					});
					return returnPs2Op(op0, op1);
				} else
					return Fail.t();
			}

			private CompileOut returnIsOp(Operand op) {
				if (result == Result.ASSIGN) {
					var opt = deOp.decomposeFunpMemory(fd, target);
					opt = opt != null ? opt : amd64.mem(mask(op).compileIsReg(target.pointer), target.start, target.size());
					if (op instanceof OpMem)
						op = em.mov(rs.mask(opt).get(op.size), op);
					em.mov(opt, op);
				} else if (result == Result.ISOP || result == Result.ISREG) {
					if (result == Result.ISREG && !(op instanceof OpReg))
						op = em.mov(rs.get(op.size), op);
					return new CompileOut(op);
				} else if (result == Result.ISSPEC)
					em.mov(pop0, op);
				else
					Fail.t();
				return new CompileOut();
			}

			private CompileOut returnPs2Op(Operand op0, Operand op1) {
				if (result == Result.ASSIGN) {
					var opt0 = deOp.decompose(fd, target.pointer, target.start, ps);
					var opt1 = deOp.decompose(fd, target.pointer, target.start + ps, ps);
					if (opt0 == null || opt1 == null) {
						var r = mask(op0, op1).compileIsReg(target.pointer);
						opt0 = amd64.mem(r, target.start, ps);
						opt1 = amd64.mem(r, target.start + ps, ps);
					}
					if (op0 instanceof OpMem)
						op0 = em.mov(rs.mask(opt0, opt1, op1).get(op0.size), op0);
					em.mov(opt0, op0);
					if (op1 instanceof OpMem)
						op1 = em.mov(rs.mask(opt1).get(op1.size), op1);
					em.mov(opt1, op1);
				} else if (result == Result.PS2OP || result == Result.PS2REG) {
					if (result == Result.PS2REG && !(op0 instanceof OpReg))
						op0 = em.mov(rs.mask(op1).get(op0.size), op0);
					if (result == Result.PS2REG && !(op1 instanceof OpReg))
						op1 = em.mov(rs.mask(op0).get(op1.size), op1);
					return new CompileOut(op0, op1);
				} else if (result == Result.PS2SPEC) {
					var r = rs.mask(op1, pop1).get(pop0);
					em.mov(r, op0);
					em.mov(pop1, op1);
					em.mov(pop0, r);
				} else
					Fail.t();
				return new CompileOut();
			}

			private CompileOut returnDontCare() {
				if (result == Result.ISOP || result == Result.ISREG)
					return new CompileOut(i_eax);
				else if (result == Result.PS2OP || result == Result.PS2REG)
					return new CompileOut(p2_eax, p2_edx);
				else
					return new CompileOut();
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
					compileIsLoad(lhs);
					opResult = compileIsOp(rhs);
				}

				if (opResult == null && operator == TermOp.DIVIDE && numRhs != null && Integer.bitCount(numRhs) == 1)
					em.shiftImm(Insn.SHR, opResult = compileIsLoad(rhs), Integer.numberOfTrailingZeros(numRhs));

				if (opResult == null)
					if (operator == TermOp.DIVIDE)
						opResult = compileDivMod(lhs, rhs, eax, edx);
					else if (operator == TermOp.MODULO)
						opResult = compileDivMod(lhs, rhs, edx, eax);
					else if (operator == TermOp.MINUS_) {
						var pair = compileCommutativeTree(Insn.SUB, assoc, lhs, rhs);
						if ((opResult = pair.t1) == rhs)
							em.emit(amd64.instruction(Insn.NEG, opResult));
					} else if (setInsn != null) {
						var pair = compileCommutativeTree(Insn.CMP, assoc, lhs, rhs);
						em.emit(amd64.instruction(pair.t0 == lhs ? setInsn : setRevInsn, opResult = isOutSpec ? pop0 : rs.get(1)));
					} else if (shInsn != null) {
						var op0 = compileIsLoad(lhs);
						if (numRhs != null)
							em.emit(amd64.instruction(shInsn, op0, amd64.imm(numRhs, 1)));
						else
							saveRegs(c1 -> {
								var opRhs = c1.mask(op0).compileIsSpec(rhs, ecx);
								em.emit(amd64.instruction(shInsn, op0, opRhs));
							}, ecx);
						opResult = op0;
					} else
						opResult = compileCommutativeTree(insn, assoc, lhs, rhs).t1;

				return opResult;
			}

			private Operand compileDivMod(Funp lhs, Funp rhs, OpReg r0, OpReg r1) {
				var opResult = isOutSpec ? pop0 : rs.get(r0);
				Sink<Compile1> sink0 = c1 -> {
					c1.compileIsSpec(lhs, eax);
					var opRhs0 = c1.mask(eax).compileIsOp(rhs);
					var opRhs1 = !(opRhs0 instanceof OpImm) ? opRhs0 : c1.rs.mask(eax, edx).get(is);
					em.mov(opRhs1, opRhs0);
					em.mov(edx, amd64.imm(0l));
					em.emit(amd64.instruction(Insn.IDIV, opRhs1));
					em.mov(opResult, r0);
				};
				Sink<Compile1> sink1 = rs.contains(r0) ? c1 -> c1.saveRegs(sink0, r0) : sink0;
				saveRegs(sink1, r1);
				return opResult;
			}

			private FixieFun4<Insn, Insn, Funp, Funp, Boolean> compileCmpJmp(Operand label) {
				return (insn, revInsn, lhs, rhs) -> {
					var pair = compileCommutativeTree(Insn.CMP, Assoc.RIGHT, lhs, rhs);
					em.emit(amd64.instruction(pair.t0 == lhs ? insn : revInsn, label));
					return true;
				};
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
				else if (!(opLhs instanceof OpImm) && opRhs instanceof OpImm)
					if (insn == Insn.CMP && opLhs != null) {
						em.emit(amd64.instruction(insn, opLhs, opRhs));
						return Pair.of(lhs, null);
					} else
						return Pair.of(lhs, em.emitRegInsn(insn, compileIsLoad(lhs), opRhs));
				else if (opLhs instanceof OpImm && !(opRhs instanceof OpImm))
					if (insn == Insn.CMP && opRhs != null) {
						em.emit(amd64.instruction(insn, opRhs, opLhs));
						return Pair.of(rhs, null);
					} else
						return Pair.of(rhs, em.emitRegInsn(insn, compileIsLoad(rhs), opLhs));
				else if (opLhs != null)
					return Pair.of(rhs, em.emitRegInsn(insn, compileIsLoad(rhs), opLhs));
				else if (opRhs != null)
					return Pair.of(lhs, em.emitRegInsn(insn, compileIsLoad(lhs), opRhs));
				else {
					var isRightAssoc = assoc == Assoc.RIGHT;
					var first = isRightAssoc ? rhs : lhs;
					var second = isRightAssoc ? lhs : rhs;
					var op0 = compileIsLoad(first);
					var op1 = mask(op0).compileIsOp(second);
					return Pair.of(first, em.emitRegInsn(insn, op0, op1));
				}
			}

			private Operand compileRoutine(Sink<Compile1> sink) {
				return compileBlock(c -> {
					var em = c.em;
					em.emit(amd64.instruction(Insn.PUSH, ebp));
					if (isUseEbp)
						em.mov(ebp, esp);
					sink.sink(c.new Compile1(registerSet, 0));
					em.emit(amd64.instruction(Insn.POP, ebp));
					em.emit(amd64.instruction(Insn.RET));
				});
			}

			private void compileGlobal(Integer size, Mutable<Operand> address) {
				address.update(compileBlock(c -> c.em.emit(amd64.instruction(Insn.DS, amd64.imm(size)))));
			}

			private Operand compileBlock(Sink<Compile0> sink) {
				var refLabel = em.label();
				em.spawn(em1 -> {
					em1.emit(amd64.instruction(Insn.LABEL, refLabel));
					sink.sink(new Compile0(result, em1, target, pop0, pop1));
				});
				return refLabel;
			}

			private void compileInstruction(Insn insn, Operand op0, Funp f1) {
				var op1 = deOp.decomposeNumber(fd, f1);
				compileInstruction(insn, op0, op1 != null ? op1 : mask(op0).compileIsOp(f1));
			}

			private void compileInstruction(Insn insn, Operand op0, Operand op1) {
				if (op0 instanceof OpMem && op1 instanceof OpMem || op0 instanceof OpImm) {
					var oldOp1 = op1;
					em.emit(amd64.instruction(Insn.MOV, op1 = rs.mask(op0).get(op1.size), oldOp1));
				}
				em.emit(amd64.instruction(insn, op0, op1));
			}

			private OpReg compileRegInstruction(Insn insn, OpReg op0, Operand op1, Funp f1) {
				return em.emitRegInsn(insn, op0, op1 != null ? op1 : mask(op0).compileIsOp(f1));
			}

			private void compileInvoke(Funp n) {
				var out = compilePs2Op(n);
				Operand op;
				if (!new RegisterSet().mask(out.op1).contains(ebp))
					op = out.op1;
				else
					op = em.mov(rs.mask(out.op0).get(ps), out.op1);
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
					compileIsSpec(node, op);
				else
					compileAllocStack(size, FunpDontCare.of(), null, c1 -> {
						var fd1 = c1.fd;
						c1.compileAssign(node, frame(fd1, fd1 + size));
						em.mov(op, compileFrame(fd1, size));
						return new CompileOut(op);
					});
				return op;
			}

			private OpReg compileIsLoad(Funp node) {
				return isOutSpec ? compileIsSpec(node, pop0) : compileIsReg(node);
			}

			private OpMem compileFrame(int start, int size) {
				OpMem op = deOp.decompose(fd, Funp_.framePointer, start, size);
				return op != null ? op : Fail.t();
			}

			private void compileAssign(Funp n, FunpMemory target) {
				new Compile0(Result.ASSIGN, em, target, null, null).new Compile1(rs, fd).compile(n);
			}

			private Operand compileIsOp(Funp n) {
				return new Compile0(Result.ISOP, em).new Compile1(rs, fd).compile(n).op0;
			}

			private OpReg compileIsReg(Funp n) {
				return (OpReg) new Compile0(Result.ISREG, em).new Compile1(rs, fd).compile(n).op0;
			}

			private OpReg compileIsSpec(Funp n, OpReg op) {
				new Compile0(Result.ISSPEC, em, null, op, null).new Compile1(rs, fd).compile(n);
				return op;
			}

			private CompileOut compilePs2Op(Funp n) {
				return new Compile0(Result.PS2OP, em).new Compile1(rs, fd).compile(n);
			}

			private CompileOut compilePs2Spec(Funp n, OpReg op0, OpReg op1) {
				new Compile0(Result.PS2SPEC, em, null, op0, op1).new Compile1(rs, fd).compile(n);
				return new CompileOut(pop0, pop1);
			}

			private OpReg compileCompare(OpReg r0, int start0, OpReg r1, int start1, int size, boolean isEq) {
				var opResult = isOutSpec ? pop0 : rs.mask(ecx, esi, edi).get(Funp_.booleanSize);
				saveRegs(c1 -> {
					var endLabel = em.label();
					var neqLabel = em.label();
					var r = rs.mask(r0, edi).get(esi);
					em.lea(r, amd64.mem(r1, start1, is));
					em.lea(edi, amd64.mem(r0, start0, is));
					em.mov(esi, r);
					em.mov(ecx, amd64.imm(size / 4, is));
					em.emit(amd64.instruction(Insn.REPE));
					em.emit(amd64.instruction(Insn.CMPSD));
					em.emit(amd64.instruction(Insn.JNE, neqLabel));
					for (var i = 0; i < size % 4; i++) {
						em.emit(amd64.instruction(Insn.CMPSB));
						em.emit(amd64.instruction(Insn.JNE, neqLabel));
					}
					em.emit(amd64.instruction(Insn.LABEL, neqLabel));
					em.emit(amd64.instruction(Insn.SETE, opResult));
					em.emit(amd64.instruction(Insn.LABEL, endLabel));
				}, ecx, esi, edi);
				return opResult;
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
		ISOP, // put value to an operand (r/m or immediate)
		ISREG, // put value to a register operand
		ISSPEC, // put value to a specified operand
		PS2OP, // put value to an operand pair
		PS2REG, // put value to a register operand pair
		PS2SPEC, // put value to a specified operand pair
	};

}
