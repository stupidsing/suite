package suite.funp;

import static java.util.Map.entry;

import java.util.Map;

import suite.adt.pair.Fixie_.FixieFun4;
import suite.assembler.Amd64.Insn;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpTree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.streamlet.FunUtil.Source;

public class P4JumpIf {

	private FixieFun4<Insn, Insn, Funp, Funp, Boolean> cmpJmp;

	private Map<TermOp, Insn> jxxInsnByOp = Map.ofEntries( //
			entry(TermOp.EQUAL_, Insn.JE), //
			entry(TermOp.LE____, Insn.JLE), //
			entry(TermOp.LT____, Insn.JL), //
			entry(TermOp.NOTEQ_, Insn.JNE));

	private Map<TermOp, Insn> jnxInsnByOp = Map.ofEntries( //
			entry(TermOp.EQUAL_, Insn.JNE), //
			entry(TermOp.LE____, Insn.JG), //
			entry(TermOp.LT____, Insn.JGE), //
			entry(TermOp.NOTEQ_, Insn.JE));

	private Map<TermOp, Insn> jxxRevInsnByOp = Map.ofEntries( //
			entry(TermOp.EQUAL_, Insn.JE), //
			entry(TermOp.LE____, Insn.JGE), //
			entry(TermOp.LT____, Insn.JG), //
			entry(TermOp.NOTEQ_, Insn.JNE));

	private Map<TermOp, Insn> jnxRevInsnByOp = Map.ofEntries( //
			entry(TermOp.EQUAL_, Insn.JNE), //
			entry(TermOp.LE____, Insn.JL), //
			entry(TermOp.LT____, Insn.JLE), //
			entry(TermOp.NOTEQ_, Insn.JE));

	public P4JumpIf(FixieFun4<Insn, Insn, Funp, Funp, Boolean> cmpJmp) {
		this.cmpJmp = cmpJmp;
	}

	public class JumpIf {
		private Operator operator;
		private Funp left, right;
		private Insn jnx, jxx, jxxRev, jnxRev;

		public JumpIf(Funp node) {
			var tree = node.cast(FunpTree.class);
			operator = tree != null ? tree.operator : null;
			left = tree != null ? tree.left : null;
			right = tree != null ? tree.right : null;
			this.jnx = operator != null ? jnxInsnByOp.get(operator) : null;
			this.jxx = operator != null ? jxxInsnByOp.get(operator) : null;
			this.jnxRev = operator != null ? jnxRevInsnByOp.get(operator) : null;
			this.jxxRev = operator != null ? jxxRevInsnByOp.get(operator) : null;
		}

		public Source<Boolean> jnxIf() {
			if (operator == TermOp.BIGAND) {
				var r0 = new JumpIf(left).jnxIf();
				var r1 = new JumpIf(right).jnxIf();
				return r0 != null && r1 != null ? () -> r0.source() && r1.source() : null;
			} else if (operator == TermOp.NOTEQ_ && right instanceof FunpBoolean && ((FunpBoolean) right).b)
				return new JumpIf(left).jxxIf();
			else if (jnx != null)
				return () -> cmpJmp.apply(jnx, jnxRev, left, right);
			else
				return null;
		}

		public Source<Boolean> jxxIf() {
			if (operator == TermOp.BIGOR_) {
				var r0 = new JumpIf(left).jxxIf();
				var r1 = new JumpIf(right).jxxIf();
				return r0 != null && r1 != null ? () -> r0.source() && r1.source() : null;
			} else if (operator == TermOp.NOTEQ_ && right instanceof FunpBoolean && ((FunpBoolean) right).b)
				return new JumpIf(left).jnxIf();
			else if (jxx != null)
				return () -> cmpJmp.apply(jxx, jxxRev, left, right);
			else
				return null;
		}
	}

}
