package suite.funp;

import static suite.util.Friends.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import suite.assembler.Amd64;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpImm;
import suite.assembler.Amd64.OpImmLabel;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.Read;

public class P4Emit {

	private static Amd64 amd64 = Amd64.me;
	private static int is = Funp_.integerSize;

	private Sink<Instruction> emit;
	private List<Block> blocks = new ArrayList<>();

	public static class Block {
		public final OpImmLabel label;
		public final List<Instruction> instructions;
		public final OpImmLabel out;

		private Block(OpImmLabel label, List<Instruction> instructions, OpImmLabel out) {
			this.label = label;
			this.instructions = instructions;
			this.out = out;
		}
	}

	public static Block generate(Sink<P4Emit> sink, OpImmLabel out) {
		var list = new ArrayList<Instruction>();
		var emit = new P4Emit(list::add);

		var label = emit.label();
		emit.emit(Insn.LABEL, label);
		sink.sink(emit);

		List<Block> blocks_ = emit.blocks;
		var blockByLabel = Read.from(blocks_).toMap(block -> block.label);
		var set = new HashSet<OpImmLabel>();

		for (var block : blocks_) {
			var label_ = block.label;

			while (label_ != null)
				if (set.add(label_)) {
					var nextBlock = blockByLabel.get(label_);
					for (var instruction : nextBlock.instructions)
						list.add(instruction);
					label_ = nextBlock.out;
				} else {
					list.add(amd64.instruction(Insn.JMP, label_));
					label_ = null;
				}
		}

		return new Block(label, list, out);
	}

	private P4Emit(Sink<Instruction> emit) {
		this.emit = emit;
	}

	public OpReg emitRegInsn(Insn insn, OpReg op0, Operand op1) {
		if (op1 instanceof OpImm) {
			var i = ((OpImm) op1).imm;
			if (insn == Insn.ADD)
				addImm(op0, i);
			else if (insn == Insn.AND)
				andImm(op0, i);
			else if (insn == Insn.IMUL)
				imulImm(op0, i);
			else if (insn == Insn.OR)
				orImm(op0, i);
			else if (insn == Insn.SHL)
				shiftImm(insn, op0, i);
			else if (insn == Insn.SHR)
				shiftImm(insn, op0, i);
			else if (insn == Insn.SUB)
				addImm(op0, -i);
			else if (insn == Insn.XOR)
				xorImm(op0, i);
			else
				emit(amd64.instruction(insn, op0, op1));
		} else
			emit(amd64.instruction(insn, op0, op1));
		return op0;
	}

	public void addImm(Operand op0, long i) {
		if (i == -1l)
			emit(amd64.instruction(Insn.DEC, op0));
		else if (i == 1l)
			emit(amd64.instruction(Insn.INC, op0));
		else if (i != 0l)
			emit(amd64.instruction(Insn.ADD, op0, imm(i)));
	}

	private void andImm(Operand op0, long i) {
		if (i != -1l)
			emit(amd64.instruction(Insn.AND, op0, imm(i)));
	}

	private void orImm(Operand op0, long i) {
		if (i != 0l)
			emit(amd64.instruction(Insn.OR, op0, imm(i)));
	}

	private void xorImm(Operand op0, long i) {
		if (i == -1l)
			emit(amd64.instruction(Insn.NOT, op0));
		else if (i != 0l)
			emit(amd64.instruction(Insn.XOR, op0, imm(i)));
	}

	private void imulImm(OpReg r0, long i) {
		if (i != 1l)
			if (Long.bitCount(i) == 1)
				shiftImm(Insn.SHL, r0, Long.numberOfTrailingZeros(i));
			else
				emit(amd64.instruction(Insn.IMUL, r0, r0, imm(i)));
	}

	public void shiftImm(Insn insn, Operand op0, long z) {
		if (z != 0l)
			emit(amd64.instruction(insn, op0, amd64.imm8(z)));
	}

	public void lea(Operand op0, OpMem op1) {
		var op = lea(op1);
		if (op != null)
			mov(op0, op);
		else
			emit(amd64.instruction(Insn.LEA, op0, op1));
	}

	public Operand lea(OpMem op) {
		if (op.baseReg < 0 && op.indexReg < 0)
			return amd64.imm(op.disp.imm, is);
		else if (op.indexReg < 0 && op.disp.imm == 0)
			return amd64.reg32[op.baseReg];
		else
			return null;
	}

	public <T extends Operand> T mov(T op0, Operand op1) {
		if (op0.size != op1.size)
			fail();
		else if (op0 != op1)
			if (op0 instanceof OpReg && op1 instanceof OpImm && ((OpImm) op1).imm == 0 && !(op1 instanceof OpImmLabel))
				emit(amd64.instruction(Insn.XOR, op0, op0));
			else
				emit(amd64.instruction(Insn.MOV, op0, op1));
		return op0;
	}

	private Operand imm(long i) {
		return Byte.MIN_VALUE <= i && i <= Byte.MAX_VALUE ? amd64.imm8(i) : amd64.imm(i, is);
	}

	public void emit(Insn insn, Operand... ops) {
		emit(amd64.instruction(insn, ops));
	}

	public void emit(Instruction instruction) {
		emit.sink(instruction);
	}

	public OpImmLabel spawn(Sink<P4Emit> sink) {
		var block = generate(sink, null);
		blocks.add(block);
		return block.label;
	}

	public OpImmLabel label() {
		var op = amd64.new OpImmLabel();
		op.size = Funp_.pointerSize;
		return op;
	}

}
