package suite.asm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import suite.BindArrayUtil.Pattern;
import suite.Suite;
import suite.adt.pair.Pair;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.util.Rewrite;
import suite.node.util.TreeUtil;
import suite.util.Fail;

public class StackAssembler {

	private Rewrite rw = new Rewrite();

	private Node rsOp0 = Atom.of("$0");
	private Node rsOp1 = Atom.of("$1");
	private Node[] registers = { Atom.of("EAX"), Atom.of("EBX"), Atom.of("ESI") };

	private Pattern FRBGN_ = Suite.pattern("FR-BEGIN ()");
	private Pattern FREND_ = Suite.pattern("FR-END ()");
	private Pattern FRGET_ = Suite.pattern("FR-GET .0");
	private Pattern FRPOP_ = Suite.pattern("FR-POP .0");
	private Pattern FRPOPN = Suite.pattern("FR-POPN .0");
	private Pattern FRPSH_ = Suite.pattern("FR-PUSH .0");
	private Pattern FRPSHN = Suite.pattern("FR-PUSHN .0");
	private Pattern LET___ = Suite.pattern("LET (.0, .1)");
	private Node RPOP__ = Atom.of("R-");
	private Node RPSH__ = Atom.of("R+");
	private Node RRESTA = Atom.of("RRESTORE-ALL");
	private Node RSAVEA = Atom.of("RSAVE-ALL");

	public final Assembler assembler;

	public StackAssembler(int bits) {
		assembler = new Assembler(bits, false, this::preassemble);
	}

	private List<Pair<Reference, Node>> preassemble(List<Pair<Reference, Node>> lnis0) {
		List<Pair<Reference, Node>> lnis1 = new ArrayList<>();
		Deque<int[]> deque = new ArrayDeque<>();
		Trail trail = new Trail();
		int fs = 0, rs = 0;

		for (Pair<Reference, Node> lni0 : lnis0) {
			var node0 = lni0.t1;
			Node node1;
			Node[] m;

			if ((m = FRBGN_.match(node0)) != null) {
				deque.push(new int[] { fs, rs, });
				fs = 0;
				rs = 0;
				node1 = Atom.NIL;
			} else if ((m = FREND_.match(node0)) != null) {
				if (fs != 0)
					node1 = Fail.t("unbalanced frame stack in subroutine definition");
				else if (rs != 0)
					node1 = Fail.t("unbalanced register stack in subroutine definition");
				else {
					int[] arr = deque.pop();
					fs = arr[0];
					rs = arr[1];
				}
				node1 = Atom.NIL;
			} else if ((m = FRGET_.match(node0)) != null)
				if (Binder.bind(m[0], Int.of(-fs), trail))
					node1 = Atom.NIL;
				else
					node1 = Fail.t("cannot bind local variable offset");
			else if ((m = FRPOP_.match(node0)) != null) {
				fs -= 4;
				node1 = Suite.substitute("POP .0", rewrite(rs, m[0]));
			} else if ((m = FRPOPN.match(node0)) != null) {
				Int int_ = (Int) m[0].finalNode();
				fs -= int_.number;
				node1 = Atom.NIL;
			} else if ((m = FRPSH_.match(node0)) != null) {
				fs += 4;
				node1 = Suite.substitute("PUSH .0", rewrite(rs, m[0]));
			} else if ((m = FRPSHN.match(node0)) != null) {
				Int int_ = (Int) m[0].finalNode();
				fs += int_.number;
				node1 = Atom.NIL;
			} else if ((m = LET___.match(node0)) != null)
				if (Binder.bind(m[0], Int.of(TreeUtil.evaluate(m[1])), trail))
					node1 = Atom.NIL;
				else
					node1 = Fail.t("cannot calculate expression");
			else if (node0 == RPOP__) {
				rs--;
				node1 = Atom.NIL;
			} else if (node0 == RPSH__) {
				rs++;
				node1 = Atom.NIL;
			} else if (node0 == RRESTA) {
				fs -= 4 * rs;
				for (int r = rs - 1; 0 <= r; r--)
					lnis1.add(Pair.of(new Reference(), Suite.substitute("POP .0", getRegister(r))));
				node1 = Atom.NIL;
			} else if (node0 == RSAVEA) {
				for (int r = 0; r < rs; r++)
					lnis1.add(Pair.of(new Reference(), Suite.substitute("PUSH .0", getRegister(r))));
				fs += 4 * rs;
				node1 = Atom.NIL;
			} else
				node1 = rewrite(rs, node0);

			lnis1.add(Pair.of(lni0.t0, node1));
		}

		return new PeepholeOptimizer().optimize(lnis1);
	}

	private Node rewrite(int sp, Node n) {
		if (0 <= sp - 1)
			n = rw.replace(rsOp0, getRegister(sp - 1), n);
		if (0 <= sp - 2)
			n = rw.replace(rsOp1, getRegister(sp - 2), n);
		return n;
	}

	private Node getRegister(int p) {
		return p < registers.length ? registers[p] : Fail.t("register stack overflow");
	}

}
