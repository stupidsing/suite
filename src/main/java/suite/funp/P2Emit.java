package suite.funp;

import java.util.ArrayList;
import java.util.List;

import suite.assembler.Amd64;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpTree2;
import suite.funp.P1.FunpFramePointer;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.util.FunUtil.Sink;

public class P2Emit {

	private int is = Funp_.integerSize;
	private Amd64 amd64 = Amd64.me;

	private Sink<Instruction> emit;

	public P2Emit(Sink<Instruction> emit) {
		this.emit = emit;
	}

	public OpMem decomposeOpMem(Funp n0, int size) {
		class DecomposeMult {
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
						reg = amd64.ebp;
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

	public void addImm(Operand op0, int i) {
		if (i == -1)
			emit(amd64.instruction(Insn.DEC, op0));
		else if (i == 1)
			emit(amd64.instruction(Insn.INC, op0));
		else if (i != 0)
			emit(amd64.instruction(Insn.ADD, op0, amd64.imm(i, is)));
	}

	public void andImm(Operand op0, int i) {
		if (i != -1)
			emit(amd64.instruction(Insn.AND, op0, amd64.imm(i, is)));
	}

	public void orImm(Operand op0, int i) {
		if (i != 0)
			emit(amd64.instruction(Insn.AND, op0, amd64.imm(i, is)));
	}

	public void imulImm(OpReg r0, int i) {
		if (Integer.bitCount(i) == 1)
			emit(amd64.instruction(Insn.SHL, r0, amd64.imm(Integer.numberOfTrailingZeros(i), 1)));
		else if (i != 1)
			emit(amd64.instruction(Insn.IMUL, r0, r0, amd64.imm(i, is)));
	}

	public void mov(Operand op0, Operand op1) {
		if (op0 != op1)
			emit(amd64.instruction(Insn.MOV, op0, op1));
	}

	public void emit(Instruction instruction) {
		emit.sink(instruction);
	}

	private boolean is1248(long scale_) {
		return scale_ == 1 || scale_ == 2 || scale_ == 4 || scale_ == 8;
	}

}
