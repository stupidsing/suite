package suite.funp;

import java.util.ArrayList;
import java.util.List;

import suite.assembler.Amd64;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpImm;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpTree2;
import suite.funp.P1.FunpFramePointer;
import suite.funp.P1.FunpMemory;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil2.Fun2;
import suite.util.Switch;

public class P3Emit {

	private int is = Funp_.integerSize;
	private Amd64 amd64 = Amd64.me;

	private Sink<Instruction> emit;

	public P3Emit(Sink<Instruction> emit) {
		this.emit = emit;
	}

	public Operand decomposeOperand(Funp node) {
		return new Switch<Operand>(node //
		).applyIf(FunpNumber.class, f -> {
			return amd64.imm(f.i, is);
		}).applyIf(FunpMemory.class, f -> f.apply((pointer, start, end) -> {
			return decomposeOpMem(pointer, start, end - start);
		})).result();
	}

	public OpMem decomposeOpMem(Funp n0, int disp0, int size) {
		class Decompose {
			private Operator operator;
			private List<Funp> nodes = new ArrayList<>();

			private Decompose(Operator operator) {
				this.operator = operator;
			}

			private void decompose(Funp n_) {
				FunpTree tree;
				if (n_ instanceof FunpTree && (tree = (FunpTree) n_).operator == operator) {
					decompose(tree.left);
					decompose(tree.right);
				} else
					nodes.add(n_);
			}
		}

		Fun2<Operator, Funp, List<Funp>> decompose = (operator, n_) -> {
			Decompose dec = new Decompose(operator);
			dec.decompose(n_);
			return dec.nodes;
		};

		class DecomposeMult {
			private long scale = 1;
			private OpReg reg;
			private List<Funp> mults = new ArrayList<>();

			private void decompose(Funp n0) {
				FunpTree2 tree;
				Funp r;
				for (Funp n1 : decompose.apply(TermOp.MULT__, n0))
					if (n1 instanceof FunpFramePointer && reg == null)
						reg = amd64.ebp;
					else if (n1 instanceof FunpNumber)
						scale *= ((FunpNumber) n1).i;
					else if (n1 instanceof FunpTree2 //
							&& (tree = (FunpTree2) n1).operator == TreeUtil.SHL //
							&& (r = tree.right) instanceof FunpNumber) {
						decompose(tree.left);
						scale <<= ((FunpNumber) r).i;
					} else
						mults.add(n1);
			}
		}

		class DecomposePlus {
			private OpReg baseReg = null, indexReg = null;
			private int scale = 1, disp = disp0;
			private boolean ok = is1248(size);

			private DecomposePlus(Funp n0) {
				for (Funp n1 : decompose.apply(TermOp.PLUS__, n0)) {
					DecomposeMult dec = new DecomposeMult();
					dec.decompose(n1);
					if (dec.mults.isEmpty()) {
						OpReg reg_ = dec.reg;
						long scale_ = dec.scale;
						if (reg_ != null)
							if (scale_ == 1 && baseReg == null)
								baseReg = reg_;
							else if (is1248(scale_) && indexReg == null) {
								indexReg = reg_;
								scale = (int) scale_;
							} else
								ok = false;
						else
							disp += scale_;
					} else
						ok = false;
				}
			}

			private OpMem op() {
				return ok ? amd64.mem(baseReg, indexReg, scale, disp, size) : null;
			}
		}

		return new DecomposePlus(n0).op();
	}

	public OpReg emitRegInsn(Insn insn, OpReg op0, Operand op1) {
		if (op1 instanceof OpImm) {
			long i = ((OpImm) op1).imm;
			if (insn == Insn.AND)
				andImm(op0, i);
			else if (insn == Insn.OR)
				orImm(op0, i);
			else if (insn == Insn.XOR)
				xorImm(op0, i);
			else if (insn == Insn.ADD)
				addImm(op0, i);
			else if (insn == Insn.SUB)
				addImm(op0, -i);
			else if (insn == Insn.IMUL)
				imulImm(op0, i);
			else if (insn == Insn.SHL)
				shiftImm(insn, op0, i);
			else if (insn == Insn.SHR)
				shiftImm(insn, op0, i);
			else
				emit(amd64.instruction(insn, op0, op1));
		} else
			emit(amd64.instruction(insn, op0, op1));
		return op0;
	}

	private void addImm(Operand op0, long i) {
		if (i == -1l)
			emit(amd64.instruction(Insn.DEC, op0));
		else if (i == 1l)
			emit(amd64.instruction(Insn.INC, op0));
		else if (i != 0l)
			emit(amd64.instruction(Insn.ADD, op0, amd64.imm(i, is)));
	}

	private void andImm(Operand op0, long i) {
		if (i != -1l)
			emit(amd64.instruction(Insn.AND, op0, amd64.imm(i, is)));
	}

	private void orImm(Operand op0, long i) {
		if (i != 0l)
			emit(amd64.instruction(Insn.OR, op0, amd64.imm(i, is)));
	}

	private void xorImm(Operand op0, long i) {
		if (i == -1l)
			emit(amd64.instruction(Insn.NOT, op0));
		else if (i != 0l)
			emit(amd64.instruction(Insn.XOR, op0, amd64.imm(i, is)));
	}

	private void imulImm(OpReg r0, long i) {
		if (i != 1l)
			if (Long.bitCount(i) == 1)
				shiftImm(Insn.SHL, r0, Long.numberOfTrailingZeros(i));
			else
				emit(amd64.instruction(Insn.IMUL, r0, r0, amd64.imm(i, is)));
	}

	public void shiftImm(Insn insn, Operand op0, long z) {
		if (z != 0l)
			emit(amd64.instruction(insn, op0, amd64.imm(z, 1)));
	}

	public void lea(Operand op0, OpMem op1) {
		if (op1.baseReg < 0 && op1.indexReg < 0)
			mov(op0, amd64.imm(op1.disp, is));
		else if (op1.indexReg < 0 && op1.disp == 0)
			mov(op0, amd64.reg32[op1.baseReg]);
		else
			emit(amd64.instruction(Insn.LEA, op0, op1));
	}

	public void mov(Operand op0, Operand op1) {
		if (op0 != op1)
			if (op0 instanceof OpReg && op1 instanceof OpImm && ((OpImm) op1).imm == 0)
				emit(amd64.instruction(Insn.XOR, op0, op0));
			else
				emit(amd64.instruction(Insn.MOV, op0, op1));
	}

	public void emit(Instruction instruction) {
		if (instruction.insn == null)
			throw new RuntimeException();
		emit.sink(instruction);
	}

	private boolean is1248(long scale_) {
		return scale_ == 1 || scale_ == 2 || scale_ == 4 || scale_ == 8;
	}

}
