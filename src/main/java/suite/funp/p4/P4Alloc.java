package suite.funp.p4;

import static primal.statics.Fail.fail;

import primal.adt.Fixie;
import primal.adt.Fixie_.Fixie3;
import primal.primitive.adt.pair.IntIntPair;
import suite.assembler.Amd64;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.OpImm;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.funp.FunpCfg;
import suite.funp.Funp_.Funp;
import suite.funp.p4.P4Emit.Emit;
import suite.funp.p4.P4GenerateCode.Compile0;
import suite.funp.p4.P4GenerateCode.CompileOut;

public class P4Alloc extends FunpCfg {

	private Amd64 amd64 = Amd64.me;
	private int is = integerSize;
	private int ps = pointerSize;
	private OpReg[] regsPs = amd64.regs(ps);
	private OpReg _ax = regsPs[amd64.axReg];
	private OpReg _bx = regsPs[amd64.bxReg];
	private OpReg _cx = regsPs[amd64.cxReg];
	private OpReg _dx = regsPs[amd64.dxReg];

	private OpImm countPointer;
	private OpImm labelPointer;
	private OpImm xorPointer;
	private OpImm freeChainTablePointer;
	private OpImm allocVsRoutine;

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

	public P4Alloc(FunpCfg f) {
		super(f);
	}

	public void init(Emit em) {
		OpReg bufferStart;

		if (isLongMode) {
			var regs64 = amd64.reg64;
			em.emit(Insn.MOV, amd64.rax, amd64.imm32(9));
			em.emit(Insn.XOR, amd64.rdi, amd64.rdi);
			em.emit(Insn.MOV, amd64.rsi, amd64.imm32(65536));
			em.emit(Insn.MOV, amd64.rdx, amd64.imm32(3));
			em.emit(Insn.MOV, regs64[10], amd64.imm32(0x22));
			em.emit(Insn.XOR, regs64[8], regs64[8]);
			em.emit(Insn.XOR, regs64[9], regs64[9]);
			em.emit(Insn.NOT, regs64[8]);
			em.emit(Insn.SYSCALL);
			bufferStart = amd64.rax;
		} else {
			em.emit(Insn.SUB, amd64.esp, amd64.imm32(0x18));
			em.emit(Insn.MOV, amd64.mem(amd64.esp, 0x00l, 4), amd64.imm32(0));
			em.emit(Insn.MOV, amd64.mem(amd64.esp, 0x04l, 4), amd64.imm32(0x00010000));
			em.emit(Insn.MOV, amd64.mem(amd64.esp, 0x08l, 4), amd64.imm32(3));
			em.emit(Insn.MOV, amd64.mem(amd64.esp, 0x0Cl, 4), amd64.imm32(0x22));
			em.emit(Insn.MOV, amd64.mem(amd64.esp, 0x10l, 4), amd64.imm32(0xFFFFFFFF));
			em.emit(Insn.MOV, amd64.mem(amd64.esp, 0x14l, 4), amd64.imm32(0));
			em.emit(Insn.MOV, amd64.eax, amd64.imm32(0x5A));
			em.emit(Insn.MOV, amd64.ebx, amd64.esp);
			em.emit(Insn.INT, amd64.imm8(0x80));
			em.emit(Insn.ADD, amd64.esp, amd64.imm8(0x18));
			bufferStart = amd64.eax;
		}

		countPointer = em.spawn(em1 -> em1.emit(Insn.D, amd64.imm(0l, is))); // how many used blocks out there
		labelPointer = em.spawn(em1 -> em1.emit(Insn.D, amd64.imm(0l, ps))); // start point of remaining free area
		xorPointer = em.spawn(em1 -> em1.emit(Insn.D, amd64.imm(0l, ps))); // allocation checker

		freeChainTablePointer = em.spawn(em1 -> em1.emit( //
				Insn.DS, //
				amd64.imm32(allocSizes.length * ps), //
				amd64.imm8(0l)));

		em.mov(amd64.mem(labelPointer, ps), bufferStart);

		var opRegPointer = _ax; // allocated pointer; return value
		var opOffset = _bx; // offset to the table of free chains
		var opSize = _cx; // size we want to allocate

		var allocSize = em.spawn(em1 -> {
			allocSize(em1, opRegPointer, opOffset, opSize, _dx, _bx);
			allocVsAdjust(em1, opRegPointer, opOffset);
			em1.emit(Insn.RET);
		});

		allocVsRoutine = em.spawn(em1 -> {
			var size = _ax;
			em1.addImm(size, ps);

			for (var i = 0; i < allocSizes.length; i++) {
				em1.mov(opOffset, amd64.imm(i * ps, ps));
				em1.mov(opSize, amd64.imm(allocSizes[i], ps));
				em1.emit(Insn.CMP, size, opSize);
				em1.emit(Insn.JLE, allocSize);
			}

			em1.emit(Insn.HLT, amd64.remark("ALLOC TOO LARGE"));
		});
	}

	public void deinit(Emit em) {
		em.emit(Insn.CMP, amd64.mem(countPointer, is), amd64.imm(0l, is));
		em.emit(Insn.JNZ, em.spawn(em1 -> em1.emit(Insn.HLT, amd64.remark("ALLOC MISMATCH"))));
		em.emit(Insn.CMP, amd64.mem(xorPointer, ps), amd64.imm(0l, ps));
		em.emit(Insn.JNZ, em.spawn(em1 -> em1.emit(Insn.HLT, amd64.remark("ALLOC POINTER MISMATCH"))));
	}

	// allocate with a fixed size, but allow de-allocation without specifying size
	// i.e. save the size information (free chain table index) into the allocated
	// block
	public CompileOut allocVs(Compile0 c0, int size) {
		return allocVs_(alloc_(c0, size + ps));
	}

	public CompileOut allocVs(Compile0 c0, OpReg size) {
		var opRegPointer = c0.rs.get(ps);
		if (opRegPointer != _ax)
			c0.em.emit(Insn.PUSH, _ax);
		c0.em.mov(_ax, size);
		c0.em.emit(Insn.PUSH, _bx);
		c0.em.emit(Insn.PUSH, _cx);
		c0.em.emit(Insn.PUSH, _dx);
		c0.em.emit(Insn.CALL, allocVsRoutine);
		c0.em.emit(Insn.POP, _dx);
		c0.em.emit(Insn.POP, _cx);
		c0.em.emit(Insn.POP, _bx);
		c0.em.mov(opRegPointer, _ax);
		if (opRegPointer != _ax)
			c0.em.emit(Insn.POP, _ax);
		return c0.returnOp(opRegPointer);
	}

	public void deallocVs(Compile0 c0, Funp reference) {
		deallocVs_(c0, reference);
	}

	public CompileOut alloc(Compile0 c0, int size) {
		return alloc_(c0, size).map((c1, r, index) -> c1.returnOp(r));
	}

	public void dealloc(Compile0 c0, int size, Funp reference) {
		var pair = getAllocSize(size);
		var opRegPointer = c0.compilePsReg(reference);
		deallocSize(c0, opRegPointer, amd64.imm(pair.t0 * ps, ps));
	}

	private CompileOut allocVs_(Fixie3<Compile0, OpReg, Operand> f) {
		return f.map((c1, opRegPointer, index) -> {
			allocVsAdjust(c1.em, opRegPointer, index);
			return c1.returnOp(opRegPointer);
		});
	}

	private void deallocVs_(Compile0 c0, Funp reference) {
		var opRegPointer = c0.compilePsReg(reference);
		c0.em.addImm(opRegPointer, -ps);
		deallocSize(c0, opRegPointer, amd64.mem(opRegPointer, 0, ps));
	}

	private void allocVsAdjust(Emit em, OpReg opRegPointer, Operand opOffset) {
		em.mov(amd64.mem(opRegPointer, 0, ps), opOffset);
		em.addImm(opRegPointer, ps);
	}

	private Fixie3<Compile0, OpReg, Operand> alloc_(Compile0 c0, int size) {
		var pair = getAllocSize(size);
		var opOffset = amd64.imm(pair.t0 * ps, ps);
		var opSize = amd64.imm(pair.t1, ps);
		var opRegPointer = c0.isOutSpec ? c0.pop0 : c0.rs.get(ps);
		var opRegFreeChain = c0.mask(opRegPointer).rs.get(ps);
		var opRegTransfer = c0.mask(opRegFreeChain, opRegPointer).rs.get(ps);

		allocSize(c0.em, opRegPointer, opOffset, opSize, opRegFreeChain, opRegTransfer);

		return Fixie.of(c0, opRegPointer, opOffset);
	}

	private void allocSize(Emit em0, OpReg opRegPointer, Operand opOffset, Operand opSize, OpReg opRegFreeChain, OpReg opRegTemp) {
		var labelEnd = em0.label();
		var fcp = getFcp(em0, opOffset, opRegFreeChain);

		em0.mov(opRegPointer, fcp);
		em0.emit(Insn.OR, opRegPointer, opRegPointer);
		em0.emit(Insn.JZ, em0.spawn(em1 -> {
			var pointer = amd64.mem(labelPointer, ps);
			em1.mov(opRegPointer, pointer);
			em1.emit(Insn.ADD, pointer, opSize);
		}, labelEnd));

		em0.mov(opRegTemp, amd64.mem(opRegPointer, 0, ps));
		em0.mov(fcp, opRegTemp);

		em0.label(labelEnd);
		em0.emit(Insn.INC, amd64.mem(countPointer, is));
		em0.emit(Insn.XOR, amd64.mem(xorPointer, ps), opRegPointer);
		// em0.emit(Insn.LOG, amd64.remark("ALLOC"), opRegPointer);
	}

	private void deallocSize(Compile0 c0, OpReg opRegPointer, Operand opOffset) {
		var opRegFreeChain = c0.mask(opRegPointer).rs.get(ps);
		var em0 = c0.em;

		// em0.emit(Insn.LOG, amd64.remark("FREE_"), opRegPointer);
		em0.emit(Insn.XOR, amd64.mem(xorPointer, ps), opRegPointer);
		em0.emit(Insn.DEC, amd64.mem(countPointer, is));

		var fcp = getFcp(em0, opOffset, opRegFreeChain);

		c0.mask(opRegPointer, opRegFreeChain).mov(amd64.mem(opRegPointer, 0, ps), fcp);
		em0.mov(fcp, opRegPointer);
	}

	private OpMem getFcp(Emit em, Operand opOffset, OpReg opRegFreeChain) {
		em.mov(opRegFreeChain, freeChainTablePointer);
		em.emit(Insn.ADD, opRegFreeChain, opOffset);
		return amd64.mem(opRegFreeChain, 0, ps);
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
