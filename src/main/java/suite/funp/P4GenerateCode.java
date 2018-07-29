package suite.funp;

import static java.util.Map.entry;
import static suite.util.Friends.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.Suite;
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
import suite.funp.P0.FunpDoAsm;
import suite.funp.P0.FunpDoWhile;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpError;
import suite.funp.P0.FunpIf;
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
import suite.funp.P2.FunpHeapAlloc;
import suite.funp.P2.FunpHeapDealloc;
import suite.funp.P2.FunpInvoke;
import suite.funp.P2.FunpInvoke2;
import suite.funp.P2.FunpInvokeIo;
import suite.funp.P2.FunpMemory;
import suite.funp.P2.FunpOperand;
import suite.funp.P2.FunpRoutine;
import suite.funp.P2.FunpRoutine2;
import suite.funp.P2.FunpRoutineIo;
import suite.funp.P2.FunpSaveRegisters0;
import suite.funp.P2.FunpSaveRegisters1;
import suite.node.Atom;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.primitive.Bytes;
import suite.primitive.IntMutable;
import suite.primitive.IntPrimitives.IntObj_Obj;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.FunUtil2.Sink2;
import suite.streamlet.Read;
import suite.util.Switch;

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

	private Operand labelPointer;

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
		var p = new Amd64Parse();

		return P4Emit.generate(emit -> {
			labelPointer = emit.label();

			emit.spawn(em1 -> {
				em1.emit(Insn.LABEL, labelPointer);
				em1.emit(Insn.D, amd64.imm32(0));
			});

			for (var i : Arrays.asList( //
					"SUB (ESP, +x18)", //
					"MOV (`ESP + +x00`, 0)", //
					"MOV (`ESP + +x04`, +x00010000)", //
					"MOV (`ESP + +x08`, +x00000003)", //
					"MOV (`ESP + +x0C`, +x00000022)", //
					"MOV (`ESP + +x10`, +xFFFFFFFF)", //
					"MOV (`ESP + +x14`, +x00000000)", //
					"MOV (EAX, +x0000005A)", //
					"MOV (EBX, ESP)", //
					"INT (+x80)", //
					"ADD (ESP, +x18)"))
				emit.emit(p.parse(Suite.parse(i)));

			emit.mov(ebx, labelPointer);
			emit.emit(p.parse(Suite.parse("MOV (`EBX`, EAX)")));

			if (isUseEbp)
				emit.mov(ebp, esp);
			emit.emit(Insn.CLD);
			new Compile0(Result.ISSPEC, emit, null, ebx, null).new Compile1(registerSet, 0).compile(funp);
			emit.mov(eax, amd64.imm(1, is));
			emit.emit(Insn.INT, amd64.imm8(-128));
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
				).applyIf(FunpAllocGlobal.class, f -> f.apply((size, value, expr, address) -> {
					if (!compileGlobal(size, address, value))
						compileAssign(value, FunpMemory.of(FunpOperand.of(address), 0, size));
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
					compileIsSpec(value, (OpReg) target.operand.value());
					return compile(expr);
				})).applyIf(FunpBoolean.class, f -> f.apply(b -> {
					return returnIsOp(amd64.imm(b ? 1 : 0, Funp_.booleanSize));
				})).applyIf(FunpCmp.class, f -> f.apply((op, l, r) -> {
					var isEq = op == TermOp.EQUAL_;
					var r0 = compileIsReg(l.pointer);
					var r1 = mask(r0).compileIsReg(r.pointer);
					var size0 = l.size();
					var size1 = r.size();
					return size0 == size1 ? returnIsOp(compileCompare(r0, l.start, r1, r.start, size0, isEq)) : fail();
				})).applyIf(FunpCoerce.class, f -> f.apply((from, to, expr) -> {
					var rbyte = pop1 != null && pop1.reg < 4 ? pop1 : rs.get(1);
					var reg = integerRegs[rbyte.reg];
					if (from == Coerce.BYTE) {
						compileByte(expr, reg);
						return returnIsOp(reg);
					} else if (to == Coerce.BYTE) {
						compileIsSpec(expr, reg);
						return returnIsOp(rbyte);
					} else
						return compile(expr);
				})).applyIf(FunpData.class, f -> f.apply(pairs -> {
					return returnAssign((c1, t) -> Read //
							.from2(pairs) //
							.sink((n_, ofs) -> c1.compileAssign(n_, FunpMemory.of(t.pointer, t.start + ofs.t0, t.start + ofs.t1))));
				})).applyIf(FunpDoAsm.class, f -> f.apply((assigns, asm) -> {
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
				})).applyIf(FunpDontCare.class, f -> {
					return returnDontCare();
				}).applyIf(FunpDoWhile.class, f -> f.apply((while_, do_, expr) -> {
					var loopLabel = em.label();
					var contLabel = em.label();
					var exitLabel = em.label();

					em.emit(Insn.LABEL, loopLabel);
					Source<Boolean> r;

					if ((r = new P4JumpIf(compileCmpJmp(exitLabel)).new JumpIf(while_).jnxIf()) != null && r.source())
						;
					else if ((r = new P4JumpIf(compileCmpJmp(contLabel)).new JumpIf(while_).jxxIf()) != null && r.source()) {
						em.emit(Insn.JMP, exitLabel);
						em.emit(Insn.LABEL, contLabel);
					} else
						compileJumpZero(while_, exitLabel);

					compileIsOp(do_);
					em.emit(Insn.JMP, loopLabel);
					em.emit(Insn.LABEL, exitLabel);
					return compile(expr);
				})).applyIf(FunpError.class, f -> {
					em.emit(Insn.HLT);
					return returnDontCare();
				}).applyIf(FunpFramePointer.class, t -> {
					return returnIsOp(compileFramePointer());
				}).applyIf(FunpHeapAlloc.class, f -> f.apply(size -> {
					var r0 = isOutSpec ? pop0 : rs.get(is);
					var rp = em.mov(rs.mask(r0).get(ps), labelPointer);
					em.mov(r0, amd64.mem(rp, 0x00, 4));
					em.addImm(amd64.mem(rp, 0x00, 4), size);
					return returnIsOp(r0);
				})).applyIf(FunpHeapDealloc.class, f -> f.apply((size, reference, expr) -> {
					return compile(expr);
				})).applyIf(FunpIf.class, f -> f.apply((if_, then, else_) -> {
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
						em.emit(Insn.JMP, endLabel);
						em.emit(Insn.LABEL, condLabel);
						compile1.sink(condf);
						em.emit(Insn.LABEL, endLabel);
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
					compileInvoke(routine);
					return returnIsOp(i_eax);
				})).applyIf(FunpInvoke2.class, f -> f.apply(routine -> {
					compileInvoke(routine);
					return returnPs2Op(p2_eax, p2_edx);
				})).applyIf(FunpInvokeIo.class, f -> f.apply((routine, is, os) -> {
					compileInvoke(routine);
					return returnAssign((c1, target) -> {
						var start = c1.fd + is;
						var source = FunpMemory.of(FunpFramePointer.of(), start, start + os);
						c1.compileAssign(source, target);
					});
				})).applyIf(FunpMemory.class, f -> f.apply((pointer, start, end) -> {
					var size = end - start;
					Operand op0, op1;
					if (result == Result.ASSIGN)
						return returnAssign((c1, target) -> c1.compileAssign(f, target));
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
						return fail();
				})).applyIf(FunpNumber.class, f -> f.apply(i -> {
					return returnIsOp(amd64.imm(i.value(), is));
				})).applyIf(FunpOperand.class, f -> f.apply(op -> {
					return returnIsOp(op.value());
				})).applyIf(FunpRoutine.class, f -> f.apply((frame, expr) -> {
					return returnPs2Op(compileIsOp(frame), compileRoutine(c1 -> c1.compileIsSpec(expr, i_eax)));
				})).applyIf(FunpRoutine2.class, f -> f.apply((frame, expr) -> {
					return returnPs2Op(compileIsOp(frame), compileRoutine(c1 -> c1.compilePs2Spec(expr, p2_eax, p2_edx)));
				})).applyIf(FunpRoutineIo.class, f -> f.apply((frame, expr, is, os) -> {
					// input argument, return address and EBP
					var o = ps + ps + is;
					var out = frame(o, o + os);
					return returnPs2Op(compileIsOp(frame), compileRoutine(c1 -> c1.compileAssign(expr, out)));
				})).applyIf(FunpSaveRegisters0.class, f -> f.apply((expr, saves) -> {
					var opRegs = rs.list(r -> !registerSet.isSet(r));
					var fd1 = fd;
					for (var opReg : opRegs)
						saves.value().add(Pair.of(opReg, fd1 -= is));
					em.addImm(esp, fd1 - fd);
					var out = new Compile1(rs, fd1).compile(expr);
					em.addImm(esp, fd - fd1);
					return out;
				})).applyIf(FunpSaveRegisters1.class, f -> f.apply((expr, saves) -> {
					for (var pair : saves.value())
						em.mov(compileFrame(pair.t1, is), pair.t0);

					var out = compile(expr);

					if (isOutSpec) {
						for (var pair : saves.value())
							if (pair.t0 != pop0 && pair.t0 != pop1)
								em.mov(pair.t0, compileFrame(pair.t1, is));
						return out;
					} else {
						var op0 = out.op0;
						var op1 = out.op1;
						if (op0 != null)
							op0 = em.mov(rs.contains(op0) ? rs.mask(op1).get(op0.size) : op0, op0);
						if (op1 != null)
							op1 = em.mov(rs.contains(op1) ? rs.mask(op0).get(op1.size) : op1, op1);
						for (var pair : saves.value())
							em.mov(pair.t0, compileFrame(pair.t1, is));
						return new CompileOut(op0, op1);
					}
				})).applyIf(FunpTree.class, f -> f.apply((op, lhs, rhs) -> {
					return returnIsOp(compileTree(n, op, op.assoc(), lhs, rhs));
				})).applyIf(FunpTree2.class, f -> f.apply((op, lhs, rhs) -> {
					return returnIsOp(compileTree(n, op, Assoc.RIGHT, lhs, rhs));
				})).nonNullResult();
			}

			private CompileOut returnAssign(Sink2<Compile1, FunpMemory> assign) {
				if (result == Result.ASSIGN) {
					if (0 < target.size())
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
					return fail();
			}

			private CompileOut returnDontCare() {
				if (result == Result.ISOP || result == Result.ISREG)
					return new CompileOut(i_eax);
				else if (result == Result.PS2OP || result == Result.PS2REG)
					return new CompileOut(p2_eax, p2_edx);
				else
					return new CompileOut();
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
					fail();
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
					fail();
				return new CompileOut();
			}

			private CompileOut compileAllocStack(int size, Funp value, List<Operand> opPops, Fun<Compile1, CompileOut> f) {
				var ism1 = is - 1;
				var alignedSize = (size + ism1) & ~ism1;
				var fd1 = fd - alignedSize;
				var c1 = new Compile1(rs, fd1);
				Operand op;

				if (size == is && (op = deOp.decomposeNumber(fd, value)) != null)
					em.emit(Insn.PUSH, op);
				else {
					em.addImm(esp, -alignedSize);
					c1.compileAssign(value, FunpMemory.of(Funp_.framePointer, fd1, fd1 + size));
				}
				var out = f.apply(c1);
				if (opPops != null)
					opPops.forEach(opPop -> em.emit(Insn.POP, opPop));
				else if (size == is)
					em.emit(Insn.POP, rs.mask(pop0, pop1, out.op0, out.op1).get(size));
				else
					em.addImm(esp, alignedSize);
				return out;
			}

			private void compileAssign(FunpMemory source, FunpMemory target) {
				var size = source.size();

				Sink2<Operand, Operand> mov = (op0, op1) -> {
					if (op0 instanceof OpMem && op1 instanceof OpMem) {
						var oldOp1 = op1;
						em.mov(op1 = rs.mask(op0).get(op1.size), oldOp1);
					}
					em.mov(op0, op1);
				};

				IntObj_Obj<OpMem, OpMem> shift = (disp, op) -> {
					var br = op.baseReg;
					var ir = op.indexReg;
					return amd64.mem( //
							0 <= br ? amd64.reg32[br] : null, //
							0 <= ir ? amd64.reg32[ir] : null, //
							op.scale, op.disp + disp, op.size);
				};

				Runnable moveBlock = () -> {
					var r0 = compileIsReg(target.pointer);
					var r1 = mask(r0).compileIsReg(source.pointer);
					compileMove(r0, target.start, r1, source.start, target.size());
				};

				if (size == target.size())
					if (size % is == 0 && Set.of(2, 3, 4).contains(size / is)) {
						var opt = deOp.decomposeFunpMemory(fd, target, is);
						var ops = deOp.decomposeFunpMemory(fd, source, is);

						if (opt != null && ops != null)
							for (var disp = 0; disp < size; disp += is)
								mov.sink2(shift.apply(disp, opt), shift.apply(disp, ops));
						else
							moveBlock.run();
					} else {
						var opt = deOp.decomposeFunpMemory(fd, target);
						var ops = deOp.decomposeFunpMemory(fd, source);

						if (ops != null)
							mov.sink2(opt != null ? opt : amd64.mem(compileIsReg(target.pointer), target.start, size), ops);
						else if (opt != null)
							mov.sink2(opt, ops != null ? ops : mask(opt).compileIsOp(source));
						else
							moveBlock.run();
					}
				else
					fail();
			}

			private Operand compileTree(Funp n, Object operator, Assoc assoc, Funp lhs, Funp rhs) {
				var numRhs = rhs.cast(FunpNumber.class, n_ -> n_.i.value());
				var insn = insnByOp.get(operator);
				var setInsn = setInsnByOp.get(operator);
				var setRevInsn = setRevInsnByOp.get(operator);
				var shInsn = shInsnByOp.get(operator);
				var op = deOp.decompose(fd, n, 0, is);
				Operand opResult = null;

				if (opResult == null && op != null)
					opResult = lea(op);

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
							em.emit(Insn.NEG, opResult);
					} else if (setInsn != null) {
						var pair = compileCommutativeTree(Insn.CMP, assoc, lhs, rhs);
						em.emit(pair.t0 == lhs ? setInsn : setRevInsn, opResult = isOutSpec ? pop0 : rs.get(1));
					} else if (shInsn != null) {
						var op0 = compileIsLoad(lhs);
						if (numRhs != null)
							em.emit(shInsn, op0, amd64.imm(numRhs, 1));
						else
							saveRegs(c1 -> {
								var opRhs = c1.mask(op0).compileIsSpec(rhs, ecx);
								em.emit(shInsn, op0, opRhs);
							}, ecx);
						opResult = op0;
					} else if (insn != null)
						opResult = compileCommutativeTree(insn, assoc, lhs, rhs).t1;
					else
						Funp_.fail(n, "unknown operator " + operator);

				return opResult;
			}

			private Operand compileDivMod(Funp lhs, Funp rhs, OpReg r0, OpReg r1) {
				var opResult = isOutSpec ? pop0 : rs.get(r0);
				Sink<Compile1> sink0 = c1 -> {
					c1.compileIsSpec(lhs, eax);
					var opRhs0 = c1.mask(eax).compileIsOp(rhs);
					var opRhs1 = !(opRhs0 instanceof OpImm) ? opRhs0 : c1.rs.mask(eax, edx).get(is);
					em.mov(opRhs1, opRhs0);
					em.mov(edx, amd64.imm32(0l));
					em.emit(Insn.IDIV, opRhs1);
					em.mov(opResult, r0);
				};
				Sink<Compile1> sink1 = rs.contains(r0) ? c1 -> c1.saveRegs(sink0, r0) : sink0;
				saveRegs(sink1, r1);
				return opResult;
			}

			private FixieFun4<Insn, Insn, Funp, Funp, Boolean> compileCmpJmp(Operand label) {
				return (insn, revInsn, lhs, rhs) -> {
					var pair = compileCommutativeTree(Insn.CMP, Assoc.RIGHT, lhs, rhs);
					em.emit(pair.t0 == lhs ? insn : revInsn, label);
					return true;
				};
			}

			private Pair<Funp, OpReg> compileCommutativeTree(Insn insn, Assoc assoc, Funp lhs, Funp rhs) {
				var opLhs = deOp.decomposeNumber(fd, lhs);
				var opRhs = deOp.decomposeNumber(fd, rhs);
				var opLhsReg = opLhs instanceof OpReg ? (OpReg) opLhs : null;
				var opRhsReg = opRhs instanceof OpReg ? (OpReg) opRhs : null;

				if (opLhsReg != null && !rs.contains(opLhsReg))
					return Pair.of(lhs, compileRegInstruction(insn, opLhsReg, opRhs, rhs));
				else if (opRhsReg != null && !rs.contains(opRhsReg))
					return Pair.of(rhs, compileRegInstruction(insn, opRhsReg, opLhs, lhs));
				else if (!(opLhs instanceof OpImm) && opRhs instanceof OpImm)
					if (insn == Insn.CMP && opLhs != null) {
						em.emit(insn, opLhs, opRhs);
						return Pair.of(lhs, null);
					} else
						return Pair.of(lhs, em.emitRegInsn(insn, compileIsLoad(lhs), opRhs));
				else if (opLhs instanceof OpImm && !(opRhs instanceof OpImm))
					if (insn == Insn.CMP && opRhs != null) {
						em.emit(insn, opRhs, opLhs);
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
					em.emit(Insn.PUSH, ebp);
					if (isUseEbp)
						em.mov(ebp, esp);
					sink.sink(c.new Compile1(registerSet, 0));
					em.emit(Insn.POP, ebp);
					em.emit(Insn.RET);
				});
			}

			private boolean compileGlobal(Integer size, Mutable<Operand> address, Funp node) {
				var o = new Object() {
					private List<Instruction> instructions = new ArrayList<>();
					private int blanks = 0;

					private boolean fill(int size, Funp node) {
						return new Switch<Boolean>(node //
						).applyIf(FunpCoerce.class, f -> f.apply((from, to, expr) -> {
							if (to == Coerce.BYTE)
								return fill(1, expr);
							else if (to == Coerce.NUMBER)
								return fill(is, expr);
							else if (to == Coerce.POINTER)
								return fill(ps, expr);
							else
								return fail();
						})).applyIf(FunpData.class, f -> f.apply(pairs -> {
							var offset = 0;
							var b = true;
							for (var pair : Read.from(pairs).sort((p0, p1) -> Integer.compare(p0.t1.t0, p1.t1.t0))) {
								var pos = pair.t1;
								b &= fill(pos.t0 - offset, FunpDontCare.of());
								b &= fill(pos.t1 - pos.t0, pair.t0);
								offset = pos.t1;
							}
							return b && fill(size - offset, FunpDontCare.of());
						})).applyIf(FunpDontCare.class, f -> {
							blanks += size;
							return true;
						}).applyIf(FunpNumber.class, f -> f.apply(i -> {
							return d(i, size);
						})).applyIf(Funp.class, f -> {
							return false;
						}).result();
					}

					private boolean d(IntMutable i, int size) {
						flush();
						return instructions.add(amd64.instruction(Insn.D, amd64.imm(i.value(), size)));
					}

					private void flush() {
						if (blanks != 0)
							instructions.add(amd64.instruction(Insn.DS, amd64.imm32(blanks)));
						blanks = 0;
					}
				};

				var ok = o.fill(size, node);
				o.flush();

				var block = compileBlock(c -> {
					if (ok)
						for (var instruction : o.instructions)
							c.em.emit(instruction);
					else
						c.em.emit(Insn.DS, amd64.imm32(size));
				});

				address.update(block);

				return ok;
			}

			private Operand compileBlock(Sink<Compile0> sink) {
				var refLabel = em.label();
				em.spawn(em1 -> {
					em1.emit(amd64.instruction(Insn.LABEL, refLabel));
					sink.sink(new Compile0(result, em1, target, pop0, pop1));
				});
				return refLabel;
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
				em.emit(Insn.CALL, op);
				if (isUseEbp && out.op0 != ebp)
					em.lea(ebp, amd64.mem(esp, -fd, is));
			}

			private void compileJumpZero(Funp if_, Operand label) {
				var op0 = isOutSpec ? pop0 : rs.get(is);
				compileByte(if_, op0);
				em.emit(Insn.OR, op0, op0);
				em.emit(Insn.JZ, label);

				// var r0 = compileOpReg(if_);
				// em.emit(Insn.OR, r0, r0));
				// em.emit(Insn.JZ, label));
			}

			private void compileByte(Funp n, Operand op0) {
				compileAllocStack(is, FunpNumber.ofNumber(0), List.of(op0), c1 -> {
					var fd1 = c1.fd;
					c1.compileAssign(n, frame(fd1, fd1 + Funp_.booleanSize));
					return new CompileOut();
				});
			}

			private OpReg compileFramePointer() {
				return lea(compileFrame(0, ps));
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
				var op = deOp.decompose(fd, Funp_.framePointer, start, size);
				return op != null ? op : fail();
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
					em.emit(Insn.REPE);
					em.emit(Insn.CMPSD);
					em.emit(Insn.JNE, neqLabel);
					for (var i = 0; i < size % 4; i++) {
						em.emit(Insn.CMPSB);
						em.emit(Insn.JNE, neqLabel);
					}
					em.emit(Insn.LABEL, neqLabel);
					em.emit(Insn.SETE, opResult);
					em.emit(Insn.LABEL, endLabel);
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
							em.emit(Insn.REP);
							em.emit(Insn.MOVSD);
							for (var i = 0; i < size % 4; i++)
								em.emit(Insn.MOVSB);
						}, ecx, esi, edi);
					else if (is <= size)
						sink.sink2(this, rs.mask(r0, r1).get(is));
					else if (0 < size)
						saveRegs(c1 -> sink.sink2(c1, cl), ecx);
			}

			private OpReg lea(OpMem opMem) {
				var op = em.lea(opMem);
				if (op instanceof OpReg)
					return (OpReg) op;
				else {
					var op0 = isOutSpec ? pop0 : rs.get(ps);
					em.lea(op0, opMem);
					return op0;
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
					em.emit(Insn.PUSH, op);
					saveRegs(sink, rs_.unmask(op.reg), fd_ - op.size, index + 1, opRegs);
					em.emit(Insn.POP, op);
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
