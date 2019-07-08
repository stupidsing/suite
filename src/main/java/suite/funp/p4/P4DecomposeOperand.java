package suite.funp.p4;

import java.util.ArrayList;
import java.util.List;

import suite.assembler.Amd64;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.funp.Funp_;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpNumber;
import suite.funp.P2.FunpFramePointer;
import suite.funp.P2.FunpMemory;
import suite.funp.P2.FunpOp;
import suite.funp.P2.FunpOperand;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.streamlet.FunUtil2.Fun2;

public class P4DecomposeOperand {

	private Amd64 amd64 = Amd64.me;
	private boolean isUseEbp;

	public P4DecomposeOperand(boolean isUseEbp) {
		this.isUseEbp = isUseEbp;
	}

	public Operand decomposeNumber(int fd, Funp node, int size) {
		return node.<Operand> switch_( //
		).applyIf(FunpDontCare.class, f -> {
			return amd64.regs(size)[amd64.axReg];
		}).applyIf(FunpMemory.class, f -> {
			return f.size() == size ? decomposeFunpMemory(fd, f) : null;
		}).applyIf(FunpNumber.class, f -> {
			return amd64.imm(f.i.value(), size);
		}).applyIf(FunpOperand.class, f -> f.apply(op -> {
			return op.value();
		})).result();
	}

	public OpMem decomposeFunpMemory(int fd, FunpMemory node) {
		return decomposeFunpMemory(fd, node, node.size());
	}

	public OpMem decomposeFunpMemory(int fd, FunpMemory node, int size) {
		return node.apply((pointer, start, end) -> decompose(fd, pointer, start, size));
	}

	public OpMem decompose(int fd, Funp n0, int disp0, int size) {
		class Decompose {
			private Operator operator;
			private List<Funp> nodes = new ArrayList<>();

			private Decompose(Operator operator) {
				this.operator = operator;
			}

			private void decompose(Funp n_) {
				FunpOp tree;
				if ((tree = n_.cast(FunpOp.class)) != null && tree.operator == operator) {
					decompose(tree.left);
					decompose(tree.right);
				} else
					nodes.add(n_);
			}
		}

		Fun2<Operator, Funp, List<Funp>> decompose = (operator, n_) -> {
			var dec = new Decompose(operator);
			dec.decompose(n_);
			return dec.nodes;
		};

		class DecomposeMult {
			private long scale = 1;
			private OpReg reg;
			private List<Funp> mults = new ArrayList<>();

			private void decompose(Funp n0) {
				FunpNumber number;
				FunpOp tree;
				Funp right;
				for (var n1 : decompose.apply(TermOp.MULT__, n0))
					if (n1 instanceof FunpFramePointer && isUseEbp && reg == null)
						reg = Funp_._bp;
					else if ((number = n1.cast(FunpNumber.class)) != null)
						scale *= number.i.value();
					else if ((tree = n1.cast(FunpOp.class)) != null //
							&& tree.operator == TreeUtil.SHL //
							&& (right = tree.right) instanceof FunpNumber) {
						decompose(tree.left);
						scale <<= ((FunpNumber) right).i.value();
					} else
						mults.add(n1);
			}
		}

		class DecomposeAdd {
			private OpReg baseReg = null, indexReg = null;
			private int scale = 1, disp = disp0;
			private boolean ok = isSizeOk(size);

			private DecomposeAdd(Funp n0) {
				for (var n1 : decompose.apply(TermOp.PLUS__, n0))
					if (n1 instanceof FunpFramePointer && !isUseEbp) {
						addReg(Funp_._sp, 1);
						disp -= fd;
					} else {
						var dec = new DecomposeMult();
						dec.decompose(n1);
						if (dec.mults.isEmpty()) {
							var reg_ = dec.reg;
							var scale_ = dec.scale;
							if (reg_ != null)
								addReg(reg_, scale_);
							else
								disp += scale_;
						} else
							ok = false;
					}
			}

			private void addReg(OpReg reg_, long scale_) {
				if (scale_ == 1 && baseReg == null)
					baseReg = reg_;
				else if (is1248(scale_) && indexReg == null) {
					indexReg = reg_;
					scale = (int) scale_;
				} else
					ok = false;
			}

			private OpMem op() {
				return ok ? amd64.mem(baseReg, indexReg, scale, disp, size) : null;
			}
		}

		return new DecomposeAdd(n0).op();
	}

	private boolean isSizeOk(long scale) {
		return scale == 1 || scale == 2 || scale == 4 || Funp_.isAmd64 && scale == 8;
	}

	private boolean is1248(long scale) {
		return scale == 1 || scale == 2 || scale == 4 || scale == 8;
	}

}
