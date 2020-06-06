package suite.funp.p4;

import static primal.statics.Fail.fail;

import primal.adt.Fixie;
import primal.adt.Fixie_.Fixie3;
import primal.primitive.adt.pair.IntIntPair;
import suite.assembler.Amd64;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.OpImm;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.funp.Funp_;
import suite.funp.Funp_.Funp;
import suite.funp.p4.P4Emit.Emit;
import suite.funp.p4.P4GenerateCode.Compile0;
import suite.funp.p4.P4GenerateCode.CompileOut;

public class P4Alloc {

	private Amd64 amd64 = Amd64.me;
	private int is = Funp_.integerSize;
	private int ps = Funp_.pointerSize;

	private OpImm countPointer;
	private OpImm labelPointer;
	private OpImm freeChainTablePointer;

	private int[] allocSizes = { //
			4, //
			8, 12, //
			16, 20, 24, //
			32, 40, 48, 56, //
			64, 80, 96, 112, //
			128, 160, 192, 224, //
			256, 320, 384, 448, //
			512, 640, 768, 896, //
			1024, 1280, 1536, 1792, //
			16777216, };

	public void init(Emit em, OpReg bufferStart) {
		countPointer = em.spawn(em1 -> em1.emit(Insn.D, amd64.imm(0l, is))).in;
		labelPointer = em.spawn(em1 -> em1.emit(Insn.D, amd64.imm(0l, is))).in;

		freeChainTablePointer = em.spawn(em1 -> em1.emit( //
				Insn.DS, //
				amd64.imm32(allocSizes.length * ps), //
				amd64.imm8(0l))).in;

		em.mov(amd64.mem(labelPointer, ps), bufferStart);
	}

	public void deinit(Emit em) {
		em.emit(Insn.CMP, amd64.mem(countPointer, is), amd64.imm(0l, is));
		em.emit(Insn.JNZ, em.spawn(em1 -> em1.emit(Insn.HLT, amd64.remark("ALLOC MISMATCH"))).in);
	}

	// allocate with a fixed size, but allow de-allocation without specifying size
	// i.e. save the size information (free chain table index) into the allocated
	// block
	public CompileOut allocVs(Compile0 c0, int size) {
		return alloc_(c0, size + ps).map((c1, pair, r) -> {
			c1.em.mov(amd64.mem(r, 0, ps), amd64.imm32(pair.t0 * ps));
			c1.em.addImm(r, ps);
			return c1.returnOp(r);
		});
	}

	public void deallocVs(Compile0 c0, Funp reference) {
		var ref = c0.compilePsReg(reference);
		c0.em.addImm(ref, -ps);
		dealloc_(c0, ref, amd64.mem(ref, 0, ps));
	}

	public CompileOut alloc(Compile0 c0, int size) {
		return alloc_(c0, size).map((c1, pair, r) -> c1.returnOp(r));
	}

	public void dealloc(Compile0 c0, int size, Funp reference) {
		var pair = getAllocSize(size);
		var ref = c0.compilePsReg(reference);
		dealloc_(c0, ref, amd64.imm(pair.t0 * ps, ps));
	}

	private Fixie3<Compile0, IntIntPair, OpReg> alloc_(Compile0 c0, int size) {
		var pair = getAllocSize(size);
		var rf = c0.em.mov(c0.rs.get(ps), freeChainTablePointer);
		c0.em.addImm(rf, pair.t0 * ps);
		var fcp = amd64.mem(rf, 0, ps);

		var c1 = c0.mask(fcp);
		var ra = c0.isOutSpec ? c0.pop0 : c1.rs.get(ps);
		var labelEnd = c1.em.label();

		c1.em.mov(ra, fcp);
		c1.em.emit(Insn.OR, ra, ra);
		c1.em.emit(Insn.JZ, c1.spawn(c2 -> {
			var pointer = amd64.mem(labelPointer, ps);
			c2.em.mov(ra, pointer);
			c2.em.addImm(pointer, pair.t1);
		}, labelEnd));

		c1.mask(ra).mov(fcp, amd64.mem(ra, 0, ps));
		c1.em.label(labelEnd);
		c1.em.emit(Insn.INC, amd64.mem(countPointer, is));

		return Fixie.of(c1, pair, ra);
	}

	private void dealloc_(Compile0 c0, OpReg ref, Operand opOffset) {
		c0.em.emit(Insn.DEC, amd64.mem(countPointer, is));

		var c1 = c0.mask(ref);
		var rf = c1.em.mov(c1.rs.get(ps), freeChainTablePointer);
		c1.em.emit(Insn.ADD, rf, opOffset);
		var fcp = amd64.mem(rf, 0, ps);

		var c2 = c1.mask(fcp);
		c2.mov(amd64.mem(ref, 0, ps), fcp);
		c2.mov(fcp, ref);
	}

	private IntIntPair getAllocSize(int size0) {
		var size1 = Math.max(ps, size0);

		for (var i = 0; i < allocSizes.length; i++) {
			var allocSize = allocSizes[i];
			if (size1 <= allocSize)
				return IntIntPair.of(i, allocSize);
		}
		return fail();
	}

}
