package suite.funp.p4;

import static java.util.Map.entry;

import java.util.Map;

import primal.adt.Fixie_.FixieFun5;
import primal.fp.Funs.Source;
import suite.assembler.Amd64.Insn;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpBoolean;
import suite.funp.P2.FunpOp;
import suite.node.io.TermOp;

public class P4JumpIf {

	private FixieFun5<Integer, Insn, Insn, Funp, Funp, Boolean> cmpJmp;

	private Map<TermOp, Insn> jxxInsnByOp = Map.ofEntries(
			entry(TermOp.EQUAL_, Insn.JE),
			entry(TermOp.LE____, Insn.JLE),
			entry(TermOp.LT____, Insn.JL),
			entry(TermOp.NOTEQ_, Insn.JNE));

	private Map<TermOp, Insn> jnxInsnByOp = Map.ofEntries(
			entry(TermOp.EQUAL_, Insn.JNE),
			entry(TermOp.LE____, Insn.JG),
			entry(TermOp.LT____, Insn.JGE),
			entry(TermOp.NOTEQ_, Insn.JE));

	private Map<TermOp, Insn> jxxRevInsnByOp = Map.ofEntries(
			entry(TermOp.EQUAL_, Insn.JE),
			entry(TermOp.LE____, Insn.JGE),
			entry(TermOp.LT____, Insn.JG),
			entry(TermOp.NOTEQ_, Insn.JNE));

	private Map<TermOp, Insn> jnxRevInsnByOp = Map.ofEntries(
			entry(TermOp.EQUAL_, Insn.JNE),
			entry(TermOp.LE____, Insn.JL),
			entry(TermOp.LT____, Insn.JLE),
			entry(TermOp.NOTEQ_, Insn.JE));

	public P4JumpIf(FixieFun5<Integer, Insn, Insn, Funp, Funp, Boolean> cmpJmp) {
		this.cmpJmp = cmpJmp;
	}

	public class JumpIf {
		private int opSize;
		private Object operator;
		private Funp left, right;
		private Insn jnx, jxx, jxxRev, jnxRev;

		public JumpIf(Funp node) {
			var tree = node.cast(FunpOp.class);
			if (tree != null) {
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
			if (operator == TermOp.BIGAND) {
				var r0 = new JumpIf(left).jnxIf();
				var r1 = new JumpIf(right).jnxIf();
				return r0 != null && r1 != null ? () -> r0.g() && r1.g() : null;
			} else if (operator == TermOp.NOTEQ_ && right instanceof FunpBoolean && ((FunpBoolean) right).b)
				return new JumpIf(left).jxxIf();
			else if (jnx != null)
				return () -> cmpJmp.apply(opSize, jnx, jnxRev, left, right);
			else
				return null;
		}

		public Source<Boolean> jxxIf() {
			if (operator == TermOp.BIGOR_) {
				var r0 = new JumpIf(left).jxxIf();
				var r1 = new JumpIf(right).jxxIf();
				return r0 != null && r1 != null ? () -> r0.g() && r1.g() : null;
			} else if (operator == TermOp.NOTEQ_ && right instanceof FunpBoolean && ((FunpBoolean) right).b)
				return new JumpIf(left).jnxIf();
			else if (jxx != null)
				return () -> cmpJmp.apply(opSize, jxx, jxxRev, left, right);
			else
				return null;
		}
	}

}
