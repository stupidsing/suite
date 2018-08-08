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

	private Amd64 amd64 = Amd64.me;
	private int is = Funp_.integerSize;

	private List<Block> blocks = new ArrayList<>();

	public class Block {
		public final OpImmLabel in;
		public final List<Instruction> instructions;
		public final OpImmLabel out;

		private Block(OpImmLabel in, List<Instruction> instructions, OpImmLabel out) {
			this.in = in;
			this.instructions = instructions;
			this.out = out;
		}
	}

	public class Emit {
		private Sink<Instruction> emit;

		private Emit(Sink<Instruction> emit) {
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

		public OpImmLabel spawn(Sink<Emit> sink) {
			return spawn(sink, null);
		}

		public OpImmLabel spawn(Sink<Emit> sink, OpImmLabel out) {
			return P4Emit.this.spawn(null, sink, out);
		}

		public OpImmLabel label() {
			return P4Emit.this.label();
		}
	}

	public ArrayList<Instruction> generate(OpImmLabel in, Sink<Emit> sink, OpImmLabel out) {
		var list = new ArrayList<Instruction>();
		list.add(amd64.instruction(Insn.JMP, spawn(in, sink, out)));

		var blockByLabel = Read.from(blocks).toMap(block -> block.in);
		var set = new HashSet<OpImmLabel>();

		for (var block : blocks) {
			var label = block.in;
			Block b;

			while (label != null)
				if (set.add(label) && (b = blockByLabel.get(label)) != null) {
					for (var instruction : b.instructions)
						list.add(instruction);
					label = b.out;
				} else {
					list.add(amd64.instruction(Insn.JMP, label));
					label = null;
				}
		}

		return list;
	}

	public OpImmLabel spawn(OpImmLabel in, Sink<Emit> sink, OpImmLabel out) {
		var in_ = in != null ? in : label();
		var list = new ArrayList<>(List.of(amd64.instruction(Insn.LABEL, in_)));

		sink.sink(new Emit(list::add));

		blocks.add(new Block(in_, list, out));
		return in_;
	}

	public OpImmLabel label() {
		var op = amd64.new OpImmLabel();
		op.size = Funp_.pointerSize;
		return op;
	}

}
