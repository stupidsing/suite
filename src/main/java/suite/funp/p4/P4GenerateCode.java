package suite.funp.p4;

import static java.util.Map.entry;
import static primal.statics.Fail.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import primal.MoreVerbs.Read;
import primal.adt.Fixie_.FixieFun5;
import primal.adt.Mutable;
import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Sink;
import primal.fp.Funs.Source;
import primal.fp.Funs2.Fun2;
import primal.fp.Funs2.Sink2;
import primal.primitive.IntPrim;
import primal.primitive.IntPrim.IntObj_Obj;
import primal.primitive.adt.Bytes;
import primal.primitive.adt.pair.IntObjPair;
import suite.Suite;
import suite.assembler.Amd64;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpIgnore;
import suite.assembler.Amd64.OpImm;
import suite.assembler.Amd64.OpImmLabel;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.assembler.Amd64Assemble;
import suite.assembler.Amd64Parse;
import suite.funp.Funp_;
import suite.funp.Funp_.Funp;
import suite.funp.P0.Coerce;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpCoerce;
import suite.funp.P0.FunpDoAsm;
import suite.funp.P0.FunpDoWhile;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpError;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpRemark;
import suite.funp.P2.FunpAllocGlobal;
import suite.funp.P2.FunpAllocReg;
import suite.funp.P2.FunpAllocStack;
import suite.funp.P2.FunpAssignMem;
import suite.funp.P2.FunpAssignOp;
import suite.funp.P2.FunpAssignOp2;
import suite.funp.P2.FunpCmp;
import suite.funp.P2.FunpData;
import suite.funp.P2.FunpFramePointer;
import suite.funp.P2.FunpHeapAlloc;
import suite.funp.P2.FunpHeapDealloc;
import suite.funp.P2.FunpInvoke;
import suite.funp.P2.FunpInvoke2;
import suite.funp.P2.FunpInvokeIo;
import suite.funp.P2.FunpMemory;
import suite.funp.P2.FunpOp;
import suite.funp.P2.FunpOperand;
import suite.funp.P2.FunpOperand2;
import suite.funp.P2.FunpRoutine;
import suite.funp.P2.FunpRoutine2;
import suite.funp.P2.FunpRoutineIo;
import suite.funp.P2.FunpSaveRegisters0;
import suite.funp.P2.FunpSaveRegisters1;
import suite.funp.RegisterSet;
import suite.funp.p4.P4Emit.Emit;
import suite.node.Atom;
import suite.node.io.Escaper;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.util.Switch;

public class P4GenerateCode {

	private boolean isAmd64 = Funp_.isAmd64;
	private Amd64 amd64 = Amd64.me;

	private int bs = Funp_.booleanSize;
	private int is = Funp_.integerSize;
	private int ps = Funp_.pointerSize;
	private int pushSize = Funp_.pushSize;
	private OpReg[] integerRegs = Funp_.integerRegs;
	private OpReg[] pointerRegs = Funp_.pointerRegs;
	private OpReg[] pushRegs = Funp_.pushRegs;

	private Amd64Assemble asm = new Amd64Assemble(pushSize);

	private int axReg = amd64.axReg;
	private int cxReg = amd64.cxReg;
	private int dxReg = amd64.dxReg;
	private int siReg = amd64.siReg;
	private int diReg = amd64.diReg;
	private OpReg eax = amd64.eax;
	private OpReg ebx = amd64.ebx;
	private OpReg ecx = amd64.ecx;
	private OpReg edi = amd64.edi;
	private OpReg _cx = pointerRegs[cxReg];
	private OpReg _si = pointerRegs[siReg];
	private OpReg _di = pointerRegs[diReg];
	private OpReg _bp = Funp_._bp;
	private OpReg _sp = Funp_._sp;

	private RegisterSet registerSet;
	private boolean isUseEbp;

	private OpReg p2_eax = pointerRegs[axReg];
	private OpReg p2_edx = pointerRegs[dxReg];

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

	private P4Alloc p4alloc = new P4Alloc();
	private P4DecomposeOperand p4deOp;
	private P4Emit p4emit = new P4Emit();

	public P4GenerateCode(boolean isUseEbp) { // or use ESP directly
		this.isUseEbp = isUseEbp;
		registerSet = new RegisterSet().mask(isUseEbp ? _bp : null, _sp);
		p4deOp = new P4DecomposeOperand(isUseEbp);
	}

	public Pair<List<Instruction>, Bytes> compile(int offset, Funp funp) {
		var instructions = compile0(funp);
		return Pair.of(instructions, compile1(offset, instructions, true));
	}

	private List<Instruction> compile0(Funp funp) {
		var p = new Amd64Parse();

		return p4emit.generate(p4emit.label(), em -> {
			var prolog_amd64 = List.of( //
					"MOV (RAX, DWORD +x00000009)", //
					"XOR (RDI, RDI)", //
					"MOV (RSI, DWORD +x00010000)", //
					"MOV (RDX, DWORD +x00000003)", //
					"MOV (R10, DWORD +x00000022)", //
					"XOR (R8, R8)", //
					"XOR (R9, R9)", //
					"NOT (R8)", //
					"SYSCALL ()");

			var prolog_i686 = List.of( //
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
					"ADD (ESP, +x18)");

			for (var i : isAmd64 ? prolog_amd64 : prolog_i686)
				em.emit(p.parse(Suite.parse(i)));

			p4alloc.init(em, pointerRegs[axReg]);

			if (isUseEbp)
				em.mov(_bp, _sp);
			em.emit(Insn.CLD);
			new Compile0(ISSPEC, em, null, ebx, null, registerSet, 0).compile(FunpRemark.of("main", funp));

			if (isAmd64) {
				em.mov(edi, ebx);
				em.mov(amd64.rax, amd64.imm64(0x3C));
				em.emit(Insn.SYSCALL);
			} else {
				em.mov(eax, amd64.imm32(1));
				em.emit(Insn.INT, amd64.imm8(0x80));
			}
		}, null);
	}

	private Bytes compile1(int offset, List<Instruction> instructions, boolean dump) {
		return asm.assemble(offset, instructions, dump);
	}

	public class Compile0 {
		public final Emit em;
		public final Result result;
		public final boolean isOutSpec;
		public final FunpMemory target; // only for Result.ASSIGN
		public final OpReg pop0, pop1; // only for Result.ISSPEC, PS2SPEC
		public final RegisterSet rs;
		public final int fd;

		private Compile0(Result result, Emit emit, FunpMemory target, OpReg pop0, OpReg pop1, RegisterSet rs, int fd) {
			this.em = emit;
			this.result = result;
			this.isOutSpec = result.t == Rt.SPEC;
			this.target = target;
			this.pop0 = pop0;
			this.pop1 = pop1;
			this.rs = rs;
			this.fd = fd;
		}

		// invariant: fd = ESP - EBP
		private CompileOut compile(Funp n) {
			n = compileSideEffects(n);

			return n.<CompileOut> switch_( //
			).applyIf(FunpAllocReg.class, f -> f.apply((size, value, expr, reg) -> {
				OpReg reg_;
				if (!reg.isEmpty())
					reg_ = (OpReg) reg.value();
				else {
					reg_ = rs.mask(pop0, pop1).get(size);
					reg.update(reg_);
				}
				return mask(compileLoad(value, reg_)).compile(expr);
			})).applyIf(FunpAllocStack.class, f -> f.apply((size, value, expr, offset) -> {
				return compileAllocStack(size, value, null, (c, s) -> {
					offset.update(c.fd);
					return c.compile(expr);
				});
			})).applyIf(FunpBoolean.class, f -> f.apply(b -> {
				return returnOp(amd64.imm(b ? 1 : 0, bs));
			})).applyIf(FunpCmp.class, f -> f.apply((op, l, r) -> {
				var isEq = op == TermOp.EQUAL_;
				var r0 = compileIsReg(l.pointer);
				var r1 = mask(r0).compileIsReg(r.pointer);
				var size0 = l.size();
				var size1 = r.size();
				return size0 == size1 ? returnOp(compileCompare(r0, l.start, r1, r.start, size0, isEq)) : fail();
			})).applyIf(FunpCoerce.class, f -> f.apply((fr, to, expr) -> {
				var frSize = Funp_.getCoerceSize(fr);
				var toSize = Funp_.getCoerceSize(to);
				FunpNumber number;

				if ((number = expr.cast(FunpNumber.class)) != null)
					return returnOp(amd64.imm(number.i.value(), toSize));
				else if (frSize != toSize) {
					var byteReg = pop1 != null && pop1.reg < 4 ? pop1 : rs.get(bs);
					var integerReg = integerRegs[byteReg.reg];
					var pointerReg = pointerRegs[byteReg.reg];

					if (fr == Coerce.BYTE)
						compileByte(expr, pushRegs[byteReg.reg]);
					else if (fr == Coerce.NUMBER)
						compileSpec(expr, integerReg);
					else if (fr == Coerce.NUMBERP || fr == Coerce.POINTER)
						compileSpec(expr, pointerReg);
					else
						fail();

					if (Funp_.isSigned(fr) && Funp_.isSigned(to) && frSize < toSize)
						em.emit(frSize < 4 ? Insn.MOVSX : Insn.MOVSXD, //
								amd64.regs(toSize)[byteReg.reg], //
								amd64.regs(frSize)[byteReg.reg]);

					if (to == Coerce.BYTE)
						return returnOp(byteReg);
					else if (to == Coerce.NUMBER)
						return returnOp(integerReg);
					else if (to == Coerce.NUMBERP || to == Coerce.POINTER)
						return returnOp(pointerReg);
					else
						return fail();
				} else
					return compile(expr);
			})).applyIf(FunpData.class, f -> f.apply(pairs -> {
				return returnAssign((c1, t) -> Read //
						.from2(pairs) //
						.sink((n_, ofs) -> c1.compileAssign(n_,
								FunpMemory.of(t.pointer, t.start + ofs.s, t.start + ofs.e))));
			})).applyIf(FunpDoAsm.class, f -> f.apply((assigns, asm, opResult) -> {
				var p = new Amd64Parse();
				new Object() {
					private Object assign(Compile0 c1, int i) {
						return i < assigns.size() ? assigns.get(i).map((op, f) -> {
							c1.compileLoad(f, op);
							return assign(c1.mask(op), i + 1);
						}) : this;
					}
				}.assign(this, 0);

				Read.from(asm).map(p::parse).sink(em::emit);
				return returnOp(opResult);
			})).applyIf(FunpDontCare.class, f -> {
				return returnDontCare();
			}).applyIf(FunpError.class, f -> {
				em.emit(Insn.HLT);
				return returnDontCare();
			}).applyIf(FunpFramePointer.class, t -> {
				return returnOp(compileFramePointer());
			}).applyIf(FunpHeapAlloc.class, f -> f.apply(size -> {
				if (0 <= size)
					return p4alloc.alloc(this, size);
				else
					return p4alloc.allocVs(this, size);
			})).applyIf(FunpHeapDealloc.class, f -> f.apply((size, reference, expr) -> {
				var out = compile(expr);
				var c1 = mask(pop0, pop1, out.op0, out.op1);
				if (0 <= size)
					p4alloc.dealloc(c1, size, reference);
				else
					p4alloc.deallocVs(c1, reference);
				return out;
			})).applyIf(FunpIf.class, f -> f.apply((if_, then_, else_) -> {
				Sink2<Compile0, Funp> compile0, compile1;
				Source<CompileOut> out;

				if (result.t == Rt.ASSIGN || isOutSpec) {
					compile0 = compile1 = Compile0::compile;
					out = CompileOut::new;
				} else if (result.nRegs == 1) {
					var ops = new OpReg[1];
					compile0 = (c1, node_) -> {
						var op0 = c1.compileOp(result.regSize, node_);
						ops[0] = c1.em.mov(rs.get(op0), op0);
					};
					compile1 = (c1, node_) -> c1.compileSpec(node_, ops[0]);
					out = () -> returnOp(ops[0]);
				} else if (result.nRegs == 2) {
					var ops = new OpReg[2];
					compile0 = (c1, node_) -> {
						var co1 = c1.compile2Op(result.regSize, node_);
						ops[0] = c1.em.mov(rs.mask(co1.op1).get(co1.op0), co1.op0);
						ops[1] = c1.em.mov(rs.mask(ops[0]).get(co1.op1), co1.op1);
					};
					compile1 = (c1, node_) -> c1.compile2Spec(node_, ops[0], ops[1]);
					out = () -> return2Op(ops[0], ops[1]);
				} else
					throw new RuntimeException();

				var stayLabel = em.label();
				var jumpLabel = em.label();
				var endLabel = em.label();

				Sink2<Funp, Funp> thenElse = (condt, condf) -> {
					em.jumpLabel(stayLabel, endLabel);
					spawn(stayLabel, c1 -> compile0.sink2(c1, condt), endLabel);
					spawn(jumpLabel, c1 -> compile1.sink2(c1, condf), endLabel);
				};

				var jumpIf = new P4JumpIf(compileCmpJmp(jumpLabel)).new JumpIf(if_);
				Source<Boolean> r;

				if ((r = jumpIf.jnxIf()) != null && r.g())
					thenElse.sink2(then_, else_);
				else if ((r = jumpIf.jxxIf()) != null && r.g())
					thenElse.sink2(else_, then_);
				else {
					compileJumpZero(if_, jumpLabel);
					thenElse.sink2(then_, else_);
				}

				return out.g();
			})).applyIf(FunpInvoke.class, f -> f.apply((routine, is, os, istack, ostack) -> {
				compileInvoke(routine);
				return returnOp(amd64.regs(os)[axReg]);
			})).applyIf(FunpInvoke2.class, f -> f.apply((routine, is, os, istack, ostack) -> {
				compileInvoke(routine);
				return return2Op(p2_eax, p2_edx);
			})).applyIf(FunpInvokeIo.class, f -> f.apply((routine, is, os, istack, ostack) -> {
				compileInvoke(routine);
				return returnAssign((c1, target) -> {
					var start = c1.fd + istack;
					var source = frame(start, start + ostack);
					c1.compileAssign(source, target);
				});
			})).applyIf(FunpMemory.class, f -> f.apply((pointer, start, end) -> {
				var size = end - start;
				Operand op0, op1;

				Source<IntObjPair<OpReg>> mfp = () -> {
					var ft = pointer.cast(FunpOp.class);
					var fn = ft != null && ft.operator == TermOp.PLUS__ ? ft.right.cast(FunpNumber.class) : null;
					var i = fn != null ? fn.i.value() : IntPrim.EMPTYVALUE;
					var pointer1 = i != IntPrim.EMPTYVALUE ? ft.left : pointer;
					var start1 = start + (i != IntPrim.EMPTYVALUE ? i : 0);
					return IntObjPair.of(start1, compilePsReg(pointer1));
				};

				if (result.t == Rt.ASSIGN)
					return returnAssign((c1, target) -> c1.compileAssign(f, target));
				else if (result.nRegs == 1)
					if ((op0 = p4deOp.decompose(fd, pointer, start, size)) != null)
						return returnOp(op0);
					else
						return mfp.g().map((start_, r) -> returnOp(amd64.mem(r, start_, size)));
				else if (result.nRegs == 2)
					if ((op0 = p4deOp.decompose(fd, pointer, start, ps)) != null
							&& (op1 = p4deOp.decompose(fd, pointer, start + ps, ps)) != null)
						return return2Op(op0, op1);
					else
						return mfp.g().map((p, r) -> return2Op(amd64.mem(r, p, ps), amd64.mem(r, p + ps, ps)));
				else
					return fail();
			})).applyIf(FunpNumber.class, f -> {
				return returnOp(amd64.imm(f.i.value(), is));
			}).applyIf(FunpOp.class, f -> f.apply((opSize, op, lhs0, rhs0) -> {
				var lhs1 = compileSideEffects(lhs0);
				var rhs1 = compileSideEffects(rhs0);
				var assoc = op instanceof TermOp ? ((TermOp) op).assoc() : Assoc.RIGHT;
				return returnOp(compileTree(opSize, op, assoc, lhs1, rhs1));
			})).applyIf(FunpOperand.class, f -> f.apply(op -> {
				return returnOp(op.value());
			})).applyIf(FunpOperand2.class, f -> f.apply((op0, op1) -> {
				return return2Op(op0.value(), op1.value());
			})).applyIf(FunpRemark.class, f -> f.apply((remark, expr) -> {
				em.emit(Insn.REMARK, amd64.remark("START -> " + remark));
				var out = compile(expr);
				em.emit(Insn.REMARK, amd64.remark("END ---> " + remark));
				return out;
			})).applyIf(FunpRoutine.class, f -> f.apply((frame, expr, is, os, istack, ostack) -> {
				var _ax = amd64.regs(os)[axReg];
				return compileRoutine(f, frame, c1 -> c1.compileSpec(expr, _ax));
			})).applyIf(FunpRoutine2.class, f -> f.apply((frame, expr, is, os, istack, ostack) -> {
				return compileRoutine(f, frame, c1 -> c1.compile2Spec(expr, p2_eax, p2_edx));
			})).applyIf(FunpRoutineIo.class, f -> f.apply((frame, expr, is, os, istack, ostack) -> {
				// input argument, return address and EBP
				var o = ps + ps + istack;
				var out = frame(o, o + ostack);
				return compileRoutine(f, frame, c1 -> c1.compileAssign(expr, out));
			})).applyIf(FunpSaveRegisters0.class, f -> f.apply((expr, saves) -> {
				Fun<Compile0, CompileOut> fun = c1 -> {
					var fd = c1.fd;
					var opRegs = c1.rs.list(r -> !registerSet.isMasked(r));
					var fd1 = fd;
					for (var opReg : opRegs)
						saves.value().add(Pair.of(opReg, fd1 -= opReg.size));
					var pushSize = getAlignedSize(fd - fd1);
					c1.em.addImm(_sp, -pushSize);
					var out = c1.nc(c1.rs, fd - pushSize).compile(expr);
					c1.em.addImm(_sp, pushSize);
					return out;
				};

				if (result.t != Rt.ASSIGN)
					return fun.apply(this);
				else {
					var size = target.size();
					if (size == is || size == ps)
						return returnOp(fun.apply(nc(new Result(Rt.REG, 1, size))).op0);
					else
						return compileAllocStack(ps, target.pointer, null, (c2, s) -> {
							var target1 = FunpMemory.of(s, target.start, target.end);
							return fun.apply(c2.nc(ASSIGN, target1, null, null));
						});
				}
			})).applyIf(FunpSaveRegisters1.class, f -> f.apply((expr, saves) -> {
				for (var pair : saves.value())
					em.mov(compileFrame(pair.v, pair.k.size), pair.k);

				var out0 = compile(expr);
				OpReg p0reg, p1reg;
				CompileOut out1;

				if (isOutSpec) {
					p0reg = pop0 != null ? pushRegs[pop0.reg] : null;
					p1reg = pop1 != null ? pushRegs[pop1.reg] : null;
					out1 = out0;
				} else {
					p0reg = p1reg = null;
					var op0 = out0.op0;
					var op1 = out0.op1;
					if (op0 != null)
						op0 = em.mov(rs.isAnyMasked(op0) ? rs.mask(op1).get(op0.size) : op0, op0);
					if (op1 != null)
						op1 = em.mov(rs.isAnyMasked(op1) ? rs.mask(op0).get(op1.size) : op1, op1);
					out1 = new CompileOut(op0, op1);
				}

				for (var pair : saves.value())
					if (pair.k != p0reg && pair.k != p1reg)
						em.mov(pair.k, compileFrame(pair.v, pair.k.size));

				return out1;
			})).nonNullResult();
		}

		private Funp compileSideEffects(Funp n) {
			Funp n1;
			while ((n1 = compileSideEffects_(n)) != null)
				n = n1;
			return n;
		}

		private Funp compileSideEffects_(Funp n) {
			return n.<Funp> switch_( //
			).applyIf(FunpAllocGlobal.class, f -> f.apply((size, value, expr, address) -> {
				if (!compileGlobal(size, address, value))
					compileAssign(value, FunpMemory.of(FunpOperand.of(address), 0, size));
				return expr;
			})).applyIf(FunpAssignMem.class, f -> f.apply((target, value, expr) -> {
				compileAssign(value, target);
				return expr;
			})).applyIf(FunpAssignOp.class, f -> f.apply((target, value, expr) -> {
				var operand = target.operand;
				if (operand.isEmpty())
					operand.update(compileIsReg(value));
				else
					compileSpec(value, (OpReg) operand.value());
				return expr;
			})).applyIf(FunpAssignOp2.class, f -> f.apply((target, value, expr) -> {
				var o = compilePs2Op(value);
				target.operand0.update(o.op0);
				target.operand1.update(o.op1);
				return expr;
			})).applyIf(FunpDoWhile.class, f -> f.apply((while_, do_, expr) -> {
				var loopLabel = em.label();
				var doLabel = em.label();
				var exitLabel = em.label();
				var label1 = Mutable.<OpImmLabel> nil();

				var block = em.spawn(loopLabel, em1 -> {
					var c1 = nc(em1);
					Source<Boolean> r;
					if ((r = new P4JumpIf(c1.compileCmpJmp(exitLabel)).new JumpIf(while_).jnxIf()) != null && r.g())
						label1.set(doLabel);
					else if ((r = new P4JumpIf(c1.compileCmpJmp(doLabel)).new JumpIf(while_).jxxIf()) != null && r.g())
						label1.set(exitLabel);
					else {
						c1.compileJumpZero(while_, exitLabel);
						label1.set(doLabel);
					}
				}, null);

				block.out = label1.value();
				spawn(doLabel, c1 -> c1.compileIsOp(do_), loopLabel);
				em.jumpLabel(loopLabel, exitLabel);
				return expr;
			})).result();
		}

		private CompileOut returnAssign(Sink2<Compile0, FunpMemory> assign) {
			if (result.t == Rt.ASSIGN) {
				if (0 < target.size())
					assign.sink2(this, target);
				return new CompileOut();
			} else if (result.nRegs == 1) {
				var op0 = isOutSpec ? pop0 : rs.get(result.regSize);
				var op0_ = pushRegs[op0.reg];
				compileAllocStack(result.regSize, FunpDontCare.of(), List.of(op0_), (c1, s) -> {
					assign.sink2(c1, frame(c1.fd, fd));
					return new CompileOut();
				});
				return returnOp(op0);
			} else if (result.nRegs == 2) {
				var op0 = isOutSpec ? pop0 : rs.get(result.regSize);
				var op1 = isOutSpec ? pop1 : rs.mask(op0).get(result.regSize);
				var op0_ = pushRegs[op0.reg];
				var op1_ = pushRegs[op1.reg];
				compileAllocStack(result.regSize + result.regSize, FunpDontCare.of(), List.of(op0_, op1_), (c1, s) -> {
					assign.sink2(c1, frame(c1.fd, fd));
					return new CompileOut();
				});
				return return2Op(op0, op1);
			} else
				return fail();
		}

		public CompileOut returnDontCare() {
			if (result.t == Rt.ASSIGN || result.t == Rt.SPEC)
				return new CompileOut();
			else if (result.nRegs == 1)
				return new CompileOut(amd64.ign(result.regSize));
			else if (result.nRegs == 2)
				return new CompileOut(amd64.ign(result.regSize), amd64.ign(result.regSize));
			else
				return fail();
		}

		public CompileOut returnOp(Operand op) {
			if (result.t == Rt.ASSIGN) {
				var opt0 = p4deOp.decomposeFunpMemory(fd, target);
				var opt1 = opt0 != null //
						? opt0 //
						: amd64.mem(mask(op).compilePsReg(target.pointer), target.start, target.size());
				if (op instanceof OpMem)
					op = em.mov(rs.mask(opt1).get(op.size), op);
				em.mov(opt1, op);
			} else if (result.t == Rt.OP || result.t == Rt.REG) {
				if (result.t == Rt.REG && !(op instanceof OpReg))
					op = em.mov(rs.get(op.size), op);
				return new CompileOut(op);
			} else if (result.t == Rt.SPEC)
				em.mov(pop0, op);
			else
				fail();
			return new CompileOut();
		}

		public CompileOut return2Op(Operand op0, Operand op1) {
			var size0 = op0.size;
			var size1 = op1.size;

			if (result.t == Rt.ASSIGN) {
				var opt0 = p4deOp.decompose(fd, target.pointer, target.start, size0);
				var opt1 = p4deOp.decompose(fd, target.pointer, target.start + size0, size1);
				if (opt0 == null || opt1 == null) {
					var r = mask(op0, op1).compilePsReg(target.pointer);
					opt0 = amd64.mem(r, target.start, size0);
					opt1 = amd64.mem(r, target.start + size0, size1);
				}
				if (op0 instanceof OpMem)
					op0 = em.mov(rs.mask(opt0, opt1, op1).get(size0), op0);
				em.mov(opt0, op0);
				if (op1 instanceof OpMem)
					op1 = em.mov(rs.mask(opt1).get(size1), op1);
				em.mov(opt1, op1);
			} else if (result.t == Rt.OP || result.t == Rt.REG) {
				if (result.t == Rt.REG && !(op0 instanceof OpReg))
					op0 = em.mov(rs.mask(op1).get(size0), op0);
				if (result.t == Rt.REG && !(op1 instanceof OpReg))
					op1 = em.mov(rs.mask(op0).get(size1), op1);
				return new CompileOut(op0, op1);
			} else if (result.t == Rt.SPEC) {
				if (!registerSet.mask(op1, pop1).isAnyMasked(pop0)) {
					em.mov(pop0, op0);
					em.mov(pop1, op1);
				} else {
					var r = !(op1 instanceof OpIgnore) ? rs.mask(op0, pop0).get(pop1) : amd64.ign(size0);
					em.mov(r, op1);
					em.mov(pop0, op0);
					em.mov(pop1, r);
				}
			} else
				fail();
			return new CompileOut();
		}

		private CompileOut compileAllocStack( //
				int size, //
				Funp pushValue, //
				List<Operand> opPops, //
				Fun2<Compile0, FunpMemory, CompileOut> f) {
			var alignedSize = getAlignedSize(size);
			var fd1 = fd - alignedSize;
			var c1 = nc(rs, fd1);
			Operand op;

			if (size == pushSize && (op = p4deOp.decomposeNumber(fd, pushValue, size)) != null)
				em.push(op);
			else {
				em.addImm(_sp, -alignedSize);
				c1.compileAssign(pushValue, frame(fd1, fd1 + size));
			}

			var out = f.apply(c1, frame(fd1, fd1 + size));
			var op0 = out.op0;
			var op1 = out.op1;
			var rs1 = c1.rs.mask(pop0, pop1);

			if (opPops != null)
				rs1 = rs1.mask(opPops.toArray(Operand[]::new));

			if (op0 != null && new RegisterSet().mask(op0).isAnyMasked(_bp, _sp))
				op0 = c1.em.mov(rs1.get(ps), op0);
			if (op1 != null && new RegisterSet().mask(op1).isAnyMasked(_bp, _sp))
				op1 = c1.em.mov(rs1.mask(op0).get(ps), op1);

			if (opPops != null)
				opPops.forEach(em::pop);
			else if (size == pushSize)
				em.pop(rs1.mask(op0, op1).get(size));
			else
				em.addImm(_sp, alignedSize);

			return new CompileOut(op0, op1);
		}

		private void compileAssign(FunpMemory source, FunpMemory target) {
			var size = source.size();

			IntObj_Obj<OpMem, OpMem> shift = (disp, op) -> {
				var br = op.baseReg;
				var ir = op.indexReg;
				return amd64.mem( //
						0 <= br ? pointerRegs[br] : null, //
						0 <= ir ? pointerRegs[ir] : null, //
						op.scale, op.disp.imm + disp, op.size);
			};

			Runnable moveBlock = () -> {
				var r0 = compilePsReg(target.pointer);
				var r1 = mask(r0).compilePsReg(source.pointer);
				var start0 = target.start;
				var start1 = source.start;

				if (r0 != r1 || start0 != start1)
					if (4 * pushSize < size)
						saveRegs(c1 -> {
							var r = rs.mask(r0, _di).get(_si);
							em.lea(r, amd64.mem(r1, start1, is));
							em.lea(_di, amd64.mem(r0, start0, is));
							em.mov(_si, r);
							em.mov(_cx, amd64.imm(size / Funp_.pointerSize, is));
							em.emit(Insn.REP);
							em.emit(isAmd64 ? Insn.MOVSQ : Insn.MOVSD);
							for (var i = 0; i < size % Funp_.pointerSize; i++)
								em.emit(Insn.MOVSB);
						}, _cx, _si, _di);
					else if (0 < size) {
						int p = 0, p1;
						for (var step : List.of(pushSize, is, 2, 1))
							for (; (p1 = p + step) <= size; p = p1)
								mov(amd64.mem(r0, start0 + p, step), amd64.mem(r1, start1 + p, step));
					}
			};

			if (size != target.size())
				fail();
			else if (size % pushSize == 0 && Set.of(2, 3, 4).contains(size / pushSize)) {
				var opt = p4deOp.decomposeFunpMemory(fd, target, pushSize);
				var ops = p4deOp.decomposeFunpMemory(fd, source, pushSize);

				if (opt != null && ops != null)
					for (var disp = 0; disp < size; disp += pushSize)
						mov(shift.apply(disp, opt), shift.apply(disp, ops));
				else
					moveBlock.run();
			} else {
				var opt = p4deOp.decomposeFunpMemory(fd, target);
				var ops = p4deOp.decomposeFunpMemory(fd, source);

				if (ops != null)
					mov(opt != null ? opt : amd64.mem(compilePsReg(target.pointer), target.start, size), ops);
				else if (opt != null)
					mov(opt, ops != null ? ops : mask(opt).compileOp(size, source));
				else
					moveBlock.run();
			}
		}

		// if operator is Insn.CMP, this would return a 1-byte operand.
		// otherwise a integer-sized (is) operand is returned.
		private Operand compileTree(int size, Object operator, Assoc assoc, Funp lhs, Funp rhs) {
			var f = FunpOp.of(size, operator, lhs, rhs);
			var regs = amd64.regs(size);
			var _ax = regs[axReg];
			var _dx = regs[dxReg];

			var numRhs = rhs.cast(FunpNumber.class, n_ -> n_.i.value());
			var insn = insnByOp.get(operator);
			var setInsn = setInsnByOp.get(operator);
			var setRevInsn = setRevInsnByOp.get(operator);
			var shInsn = shInsnByOp.get(operator);
			var op = p4deOp.decompose(fd, f, 0, size);
			Operand opResult = null;

			if (opResult == null && op != null && size <= ps)
				opResult = regs[lea(op).reg];

			if (opResult == null && operator == TermOp.OR____) {
				compileLoad(size, lhs);
				opResult = compileOp(size, rhs);
			}

			if (opResult == null && operator == TermOp.DIVIDE && numRhs != null && Integer.bitCount(numRhs) == 1)
				em.shiftImm(Insn.SHR, opResult = compileLoad(size, rhs), Integer.numberOfTrailingZeros(numRhs));

			if (opResult == null)
				if (operator == TermOp.DIVIDE)
					opResult = compileDivMod(lhs, rhs, _ax, _dx);
				else if (operator == TermOp.MODULO)
					opResult = compileDivMod(lhs, rhs, _dx, _ax);
				else if (operator == TermOp.MINUS_) {
					var pair = compileCommutativeTree(size, Insn.SUB, assoc, lhs, rhs);
					opResult = pair.v;
					if (pair.k == rhs)
						em.emit(Insn.NEG, opResult);
				} else if (setInsn != null) {
					var pair = compileCommutativeTree(size, Insn.CMP, assoc, lhs, rhs);
					em.emit(pair.k == lhs ? setInsn : setRevInsn, opResult = isOutSpec ? pop0 : rs.get(bs));
				} else if (shInsn != null) {
					var op0 = compileLoad(size, lhs);
					if (numRhs != null)
						em.emit(shInsn, op0, amd64.imm(numRhs, 1));
					else
						saveRegs(c1 -> {
							var opRhs = c1.mask(op0).compileSpec(rhs, ecx);
							em.emit(shInsn, op0, amd64.reg8[opRhs.reg]);
						}, ecx);
					opResult = op0;
				} else if (insn != null)
					opResult = compileCommutativeTree(size, insn, assoc, lhs, rhs).v;
				else
					Funp_.fail(f, "unknown operator " + operator);

			return opResult;
		}

		private Operand compileDivMod(Funp lhs, Funp rhs, OpReg r0, OpReg r1) {
			var regs = amd64.regs(r0.size);
			var _ax = regs[axReg];
			var _dx = regs[dxReg];
			var opResult = isOutSpec ? pop0 : rs.get(r0);
			Sink<Compile0> sink0 = c1 -> {
				c1.compileSpec(lhs, _ax);
				var opRhs0 = c1.mask(_ax).compileOp(r0.size, rhs);
				var opRhs1 = !(opRhs0 instanceof OpImm) ? opRhs0 : c1.rs.mask(_ax, _dx).get(r0.size);
				em.mov(opRhs1, opRhs0);
				em.mov(_dx, amd64.imm32(0l));
				em.emit(Insn.IDIV, opRhs1);
				em.mov(opResult, r0);
			};
			Sink<Compile0> sink1 = rs.isAnyMasked(r0) ? c1 -> c1.saveRegs(sink0, r0) : sink0;
			saveRegs(sink1, r1);
			return opResult;
		}

		private FixieFun5<Integer, Insn, Insn, Funp, Funp, Boolean> compileCmpJmp(Operand label) {
			return (opSize, insn, revInsn, lhs, rhs) -> {
				var pair = compileCommutativeTree(opSize, Insn.CMP, Assoc.RIGHT, lhs, rhs);
				em.emit(pair.k == lhs ? insn : revInsn, label);
				return true;
			};
		}

		private Pair<Funp, OpReg> compileCommutativeTree(int size, Insn insn, Assoc assoc, Funp lhs, Funp rhs) {
			var opLhs = p4deOp.decomposeNumber(fd, lhs, size);
			var opRhs = p4deOp.decomposeNumber(fd, rhs, size);
			var opLhsReg = opLhs instanceof OpReg ? (OpReg) opLhs : null;
			var opRhsReg = opRhs instanceof OpReg ? (OpReg) opRhs : null;

			if (opLhsReg != null && !rs.isAnyMasked(opLhsReg))
				return Pair.of(lhs, compileRegInstruction(size, insn, opLhsReg, opRhs, rhs));
			else if (opRhsReg != null && !rs.isAnyMasked(opRhsReg))
				return Pair.of(rhs, compileRegInstruction(size, insn, opRhsReg, opLhs, lhs));
			else if (!(opLhs instanceof OpImm) && opRhs instanceof OpImm)
				if (insn == Insn.CMP && opLhs != null) {
					em.emit(insn, opLhs, opRhs);
					return Pair.of(lhs, null);
				} else
					return Pair.of(lhs, em.emitRegInsn(insn, compileLoadMasked(size, lhs, opRhs), opRhs));
			else if (opLhs instanceof OpImm && !(opRhs instanceof OpImm))
				if (insn == Insn.CMP && opRhs != null) {
					em.emit(insn, opRhs, opLhs);
					return Pair.of(rhs, null);
				} else
					return Pair.of(rhs, em.emitRegInsn(insn, compileLoadMasked(size, rhs, opLhs), opLhs));
			else if (opLhs != null)
				return Pair.of(rhs, em.emitRegInsn(insn, compileLoadMasked(size, rhs, opLhs), opLhs));
			else if (opRhs != null)
				return Pair.of(lhs, em.emitRegInsn(insn, compileLoadMasked(size, lhs, opRhs), opRhs));
			else {
				var isRightAssoc = assoc == Assoc.RIGHT;
				var first = isRightAssoc ? rhs : lhs;
				var second = isRightAssoc ? lhs : rhs;
				var op0 = compileLoadMasked(size, first, null);
				var op1 = mask(op0).compileOp(size, second);
				return Pair.of(first, em.emitRegInsn(insn, op0, op1));
			}
		}

		private OpReg compileLoadMasked(int size, Funp node, Operand opOther) {
			var rs1 = rs.mask(opOther);
			var op = isOutSpec ? compileSpec(node, rs1.get(amd64.regs(size)[pop0.reg])) : compileReg(size, node);
			return em.mov(rs.isAnyMasked(op) ? rs1.get(size) : op, op);
		}

		private CompileOut compileRoutine(Funp f, Funp frame, Sink<Compile0> sink0) {
			return return2Op(compilePsOp(frame), spawn(c -> {
				var em1 = c.em;
				em1.push(_bp);
				if (isUseEbp)
					em1.mov(_bp, _sp);
				sink0.f(c.nc(registerSet, 0));
				em1.pop(_bp);
				em1.emit(Insn.RET);
			}));
		}

		private boolean compileGlobal(int size, Mutable<Operand> address, Funp node) {
			var o = new Object() {
				private List<Instruction> instructions = new ArrayList<>();
				private int blanks = 0;

				private boolean fill(int size, Funp node) {
					return new Switch<Boolean>(node //
					).applyIf(FunpCoerce.class, f -> f.apply((from, to, expr) -> {
						return fill(Funp_.getCoerceSize(to), expr);
					})).applyIf(FunpData.class, f -> f.apply(pairs -> {
						var offset = 0;
						var b = true;
						for (var pair : Read.from(pairs).sort((p0, p1) -> Integer.compare(p0.v.s, p1.v.s))) {
							var pos = pair.v;
							b &= fill(pos.s - offset, FunpDontCare.of());
							b &= fill(pos.length(), pair.k);
							offset = pos.e;
						}
						return b && fill(size - offset, FunpDontCare.of());
					})).applyIf(FunpDontCare.class, f -> {
						blanks += size;
						return true;
					}).applyIf(FunpNumber.class, f -> {
						flush();
						return instructions.add(amd64.instruction(Insn.D, amd64.imm(f.i.value(), size)));
					}).applyIf(FunpRemark.class, f -> f.apply((remark, expr) -> {
						instructions.add(amd64.instruction(Insn.REMARK, amd64.remark(Escaper.escape(remark, '"'))));
						var o = fill(size, expr);
						return o;
					})).applyIf(Funp.class, f -> {
						return false;
					}).result();
				}

				private void flush() {
					if (blanks != 0)
						instructions.add(amd64.instruction(Insn.DS, amd64.imm32(blanks)));
					blanks = 0;
				}
			};

			var ok = o.fill(size, node);
			o.flush();

			var block = spawn(c -> {
				if (ok)
					o.instructions.forEach(c.em::emit);
				else
					c.em.emit(Insn.DS, amd64.imm32(size));
			});

			address.update(block);

			return ok;
		}

		private OpReg compileRegInstruction(int size, Insn insn, OpReg op0, Operand op1, Funp f1) {
			return em.emitRegInsn(insn, op0, op1 != null ? op1 : mask(op0).compileOp(size, f1));
		}

		private void compileInvoke(Funp n) {
			var out = compilePs2Op(n);
			Operand op;
			if (!new RegisterSet().mask(out.op1).isAnyMasked(_bp))
				op = out.op1;
			else
				op = em.mov(rs.mask(out.op0).get(ps), out.op1);
			em.mov(_bp, out.op0);
			em.emit(Insn.CALL, op);
			if (isUseEbp && out.op0 != _bp)
				em.lea(_bp, amd64.mem(_sp, -fd, is));
		}

		private void compileJumpZero(Funp if_, Operand label) {
			var op0 = rs.get(Funp_.pushSize);
			compileByte(if_, op0);
			em.emit(Insn.OR, op0, op0);
			em.emit(Insn.JZ, label);

			// var r0 = compileOpReg(if_);
			// em.emit(Insn.OR, r0, r0));
			// em.emitJump(Insn.JZ, label));
		}

		private void compileByte(Funp n, Operand op0) {
			compileAllocStack(op0.size, FunpNumber.ofNumber(0), List.of(op0), (c1, s) -> {
				var fd1 = c1.fd;
				c1.compileAssign(n, frame(fd1, fd1 + 1));
				return new CompileOut();
			});
		}

		private OpReg compileFramePointer() {
			return lea(compileFrame(0, ps));
		}

		private OpReg compileLoad(Funp node, OpReg op) {
			var size = op.size;
			if (size == is || size == ps)
				compileSpec(node, op);
			else
				compileAllocStack(size, FunpDontCare.of(), null, (c1, s) -> {
					var fd1 = c1.fd;
					c1.compileAssign(node, s);
					em.mov(op, compileFrame(fd1, size));
					return new CompileOut(op);
				});
			return op;
		}

		private OpReg compileLoad(int size, Funp node) {
			return isOutSpec ? compileSpec(node, amd64.regs(size)[pop0.reg]) : compileReg(size, node);
		}

		private OpMem compileFrame(int start, int size) {
			var op = p4deOp.decompose(fd, Funp_.framePointer, start, size);
			return op != null ? op : fail();
		}

		private void compileAssign(Funp n, FunpMemory target) {
			nc(ASSIGN, target, null, null).compile(n);
		}

		public Operand compileIsOp(Funp n) {
			return compileOp(is, n);
		}

		public OpReg compileIsReg(Funp n) {
			return compileReg(is, n);
		}

		public Operand compilePsOp(Funp n) {
			return compileOp(ps, n);
		}

		public OpReg compilePsReg(Funp n) {
			return compileReg(ps, n);
		}

		public CompileOut compilePs2Op(Funp n) {
			return compile2Op(ps, n);
		}

		private Operand compileOp(int size, Funp n) {
			return nc(new Result(Rt.OP, 1, size)).compile(n).op0;
		}

		private CompileOut compile2Op(int size, Funp n) {
			return nc(new Result(Rt.OP, 2, size)).compile(n);
		}

		private OpReg compileReg(int size, Funp n) {
			return (OpReg) nc(new Result(Rt.REG, 1, size)).compile(n).op0;
		}

		private OpReg compileSpec(Funp n, OpReg op) {
			nc(new Result(Rt.SPEC, 1, op.size), null, op, null).compile(n);
			return op;
		}

		private CompileOut compile2Spec(Funp n, OpReg op0, OpReg op1) {
			if (op0.size == op1.size) {
				nc(new Result(Rt.SPEC, 2, op0.size), null, op0, op1).compile(n);
				return new CompileOut(pop0, pop1);
			} else
				return fail();
		}

		private OpReg compileCompare(OpReg r0, int start0, OpReg r1, int start1, int size, boolean isEq) {
			var opResult = isOutSpec ? pop0 : rs.mask(_cx, _si, _di).get(bs);
			var endLabel = em.label();
			var neqLabel = spawn(c1 -> c1.em.emit(isEq ? Insn.SETE : Insn.SETNE, opResult), endLabel);

			saveRegs(c1 -> {
				var r = rs.mask(r0, _di).get(_si);
				em.lea(r, amd64.mem(r1, start1, is));
				em.lea(_di, amd64.mem(r0, start0, is));
				em.mov(_si, r);
				em.mov(_cx, amd64.imm(size / Funp_.pointerSize, is));
				em.emit(Insn.REPE);
				em.emit(isAmd64 ? Insn.CMPSQ : Insn.CMPSD);
				em.emit(Insn.JNE, neqLabel);
				for (var i = 0; i < size % Funp_.pointerSize; i++) {
					em.emit(Insn.CMPSB);
					em.emit(Insn.JNE, neqLabel);
				}
				em.jumpLabel(neqLabel, endLabel);
			}, _cx, _si, _di);

			return opResult;
		}

		// loads the address of a memory operand
		public OpReg lea(OpMem opMem) {
			var op = em.lea(opMem);
			if (op instanceof OpReg)
				return (OpReg) op;
			else {
				var op0 = isOutSpec ? pop0 : rs.get(ps);
				var opr = pointerRegs[op0.reg];
				em.lea(opr, opMem);
				return opr;
			}
		}

		// allows moving any operand to another; uses an intermediate register if required
		public <T extends Operand> T mov(T op0, Operand op1) {
			if (op0 instanceof OpMem && op1 instanceof OpMem) {
				var oldOp1 = op1;
				em.mov(op1 = rs.mask(op0, op1).get(op1.size, isAmd64), oldOp1);
			}
			return em.mov(op0, op1);
		}

		private FunpMemory frame(int start, int end) {
			return FunpMemory.of(Funp_.framePointer, start, end);
		}

		private void saveRegs(Sink<Compile0> sink, OpReg... opRegs) {
			saveRegs(sink, rs, fd, 0, opRegs);
		}

		private void saveRegs(Sink<Compile0> sink, RegisterSet rs_, int fd_, int index, OpReg... opRegs) {
			OpReg op;
			if (index < opRegs.length && rs_.isAnyMasked(op = opRegs[index])) {
				var opPush = pushRegs[op.reg];
				em.push(opPush);
				saveRegs(sink, rs_.unmask(op.reg), fd_ - opPush.size, index + 1, opRegs);
				em.pop(opPush);
			} else
				sink.f(nc(rs_, fd_));
		}

		private int getAlignedSize(int size) {
			var ism1 = pushSize - 1;
			return (size + ism1) & ~ism1;
		}

		public Compile0 mask(Operand... ops) {
			return nc(rs.mask(ops), fd);
		}

		public OpImmLabel spawn(Sink<Compile0> sink) {
			return spawn(sink, null);
		}

		public OpImmLabel spawn(Sink<Compile0> sink, OpImmLabel out) {
			return spawn(em.label(), sink, out);
		}

		public OpImmLabel spawn(OpImmLabel in, Sink<Compile0> sink, OpImmLabel out) {
			em.spawn(in, em1 -> sink.f(nc(em1)), out);
			return in;
		}

		private Compile0 nc(Result result) {
			return new Compile0(result, em, target, pop0, pop1, rs, fd);
		}

		private Compile0 nc(Result result, FunpMemory target, OpReg pop0, OpReg pop1) {
			return new Compile0(result, em, target, pop0, pop1, rs, fd);
		}

		private Compile0 nc(Emit em) {
			return new Compile0(result, em, target, pop0, pop1, rs, fd);
		}

		private Compile0 nc(RegisterSet rs, int fd) {
			return new Compile0(result, em, target, pop0, pop1, rs, fd);
		}
	}

	public class CompileOut {
		public final Operand op0, op1;

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

	private Result ASSIGN = new Result(Rt.ASSIGN, -1, -1); // assign value to certain memory region
	private Result ISSPEC = new Result(Rt.SPEC, 1, is); // put value to a specified operand

	private class Result {
		private Rt t;
		private int nRegs; // 1 or 2 registers
		private int regSize; // 4 or 8 bytes

		private Result(Rt t, int nRegs, int regSize) {
			this.t = t;
			this.nRegs = nRegs;
			this.regSize = regSize;
		}
	}

	private enum Rt {
		ASSIGN, // assign value to certain memory region
		OP, // put value to an operand
		REG, // put value to a register operand
		SPEC, // put value to a specified operand
	};

}
