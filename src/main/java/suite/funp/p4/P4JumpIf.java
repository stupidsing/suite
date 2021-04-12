package suite.funp.p4;

import static java.util.Map.entry;

import java.util.Map;

import primal.adt.Fixie_.FixieFun5;
import primal.fp.Funs.Source;
import primal.parser.Operator;
import suite.assembler.Amd64.Insn;
import suite.funp.FunpOp;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpBoolean;
import suite.funp.P2.FunpOpLr;

public class P4JumpIf {

	private FixieFun5<Integer, Insn, Insn, Funp, Funp, Boolean> cmpJmp;

	private Map<Operator, Insn> jxxInsnByOp = Map.ofEntries( //
			entry(FunpOp.EQUAL_, Insn.JE), //
			entry(FunpOp.LE____, Insn.JLE), //
			entry(FunpOp.LT____, Insn.JL), //
			entry(FunpOp.NOTEQ_, Insn.JNE));

	private Map<Operator, Insn> jnxInsnByOp = Map.ofEntries( //
			entry(FunpOp.EQUAL_, Insn.JNE), //
			entry(FunpOp.LE____, Insn.JG), //
			entry(FunpOp.LT____, Insn.JGE), //
			entry(FunpOp.NOTEQ_, Insn.JE));

	private Map<Operator, Insn> jxxRevInsnByOp = Map.ofEntries( //
			entry(FunpOp.EQUAL_, Insn.JE), //
			entry(FunpOp.LE____, Insn.JGE), //
			entry(FunpOp.LT____, Insn.JG), //
			entry(FunpOp.NOTEQ_, Insn.JNE));

	private Map<Operator, Insn> jnxRevInsnByOp = Map.ofEntries( //
			entry(FunpOp.EQUAL_, Insn.JNE), //
			entry(FunpOp.LE____, Insn.JL), //
			entry(FunpOp.LT____, Insn.JLE), //
			entry(FunpOp.NOTEQ_, Insn.JE));

	public P4JumpIf(FixieFun5<Integer, Insn, Insn, Funp, Funp, Boolean> cmpJmp) {
		this.cmpJmp = cmpJmp;
	}

	public class JumpIf {
		private int opSize;
		private Object operator;
		private Funp left, right;
		private Insn jnx, jxx, jxxRev, jnxRev;

		public JumpIf(Funp node) {
			if (node instanceof FunpOpLr tree) {
				opSize = tree.opSize;
				operator = tree.operator;
				left = tree.left;
				right = tree.right;
				jnx = jnxInsnByOp.get(operator);
				jxx = jxxInsnByOp.get(operator);
				jnxRev = jnxRevInsnByOp.get(operator);
				jxxRev = jxxRevInsnByOp.get(operator);
			}
		}

		public Source<Boolean> jnxIf() {
			if (operator == FunpOp.BIGAND) {
				var r0 = new JumpIf(left).jnxIf();
				var r1 = new JumpIf(right).jnxIf();
				return r0 != null && r1 != null ? () -> r0.g() && r1.g() : null;
			} else if (operator == FunpOp.NOTEQ_ && right instanceof FunpBoolean fb && fb.b)
				return new JumpIf(left).jxxIf();
			else if (jnx != null)
				return () -> cmpJmp.apply(opSize, jnx, jnxRev, left, right);
			else
				return null;
		}

		public Source<Boolean> jxxIf() {
			if (operator == FunpOp.BIGOR_) {
				var r0 = new JumpIf(left).jxxIf();
				var r1 = new JumpIf(right).jxxIf();
				return r0 != null && r1 != null ? () -> r0.g() && r1.g() : null;
			} else if (operator == FunpOp.NOTEQ_ && right instanceof FunpBoolean fb && fb.b)
				return new JumpIf(left).jnxIf();
			else if (jxx != null)
				return () -> cmpJmp.apply(opSize, jxx, jxxRev, left, right);
			else
				return null;
		}
	}

}
