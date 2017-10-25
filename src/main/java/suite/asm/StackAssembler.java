package suite.asm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import suite.BindArrayUtil.Match;
import suite.Suite;
import suite.adt.pair.Pair;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.util.TreeRewriter;
import suite.node.util.TreeUtil;

public class StackAssembler {

	private Node rsOp0 = Atom.of("$0");
	private Node rsOp1 = Atom.of("$1");
	private Node[] registers = { Atom.of("EAX"), Atom.of("EBX"), Atom.of("ESI") };

	private Match FRBGN_ = Suite.match("FR-BEGIN ()");
	private Match FREND_ = Suite.match("FR-END ()");
	private Match FRGET_ = Suite.match("FR-GET .0");
	private Match FRPOP_ = Suite.match("FR-POP .0");
	private Match FRPOPN = Suite.match("FR-POPN .0");
	private Match FRPSH_ = Suite.match("FR-PUSH .0");
	private Match FRPSHN = Suite.match("FR-PUSHN .0");
	private Match LET___ = Suite.match("LET (.0, .1)");
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
			Node node0 = lni0.t1;
			Node node1;
			Node[] m;

			if ((m = FRBGN_.apply(node0)) != null) {
				deque.push(new int[] { fs, rs, });
				fs = 0;
				rs = 0;
				node1 = Atom.NIL;
			} else if ((m = FREND_.apply(node0)) != null) {
				if (fs != 0)
					throw new RuntimeException("unbalanced frame stack in subroutine definition");
				else if (rs != 0)
					throw new RuntimeException("unbalanced register stack in subroutine definition");
				else {
					int[] arr = deque.pop();
					fs = arr[0];
					rs = arr[1];
				}
				node1 = Atom.NIL;
			} else if ((m = FRGET_.apply(node0)) != null)
				if (Binder.bind(m[0], Int.of(-fs), trail))
					node1 = Atom.NIL;
				else
					throw new RuntimeException("cannot bind local variable offset");
			else if ((m = FRPOP_.apply(node0)) != null) {
				fs -= 4;
				node1 = Suite.substitute("POP .0", rewrite(rs, m[0]));
			} else if ((m = FRPOPN.apply(node0)) != null) {
				Int int_ = (Int) m[0].finalNode();
				fs -= int_.number;
				node1 = Atom.NIL;
			} else if ((m = FRPSH_.apply(node0)) != null) {
				fs += 4;
				node1 = Suite.substitute("PUSH .0", rewrite(rs, m[0]));
			} else if ((m = FRPSHN.apply(node0)) != null) {
				Int int_ = (Int) m[0].finalNode();
				fs += int_.number;
				node1 = Atom.NIL;
			} else if ((m = LET___.apply(node0)) != null)
				if (Binder.bind(m[0], Int.of(TreeUtil.evaluate(m[1])), trail))
					node1 = Atom.NIL;
				else
					throw new RuntimeException("cannot calculate expression");
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
			n = new TreeRewriter().replace(rsOp0, getRegister(sp - 1), n);
		if (0 <= sp - 2)
			n = new TreeRewriter().replace(rsOp1, getRegister(sp - 2), n);
		return n;
	}

	private Node getRegister(int p) {
		if (p < registers.length)
			return registers[p];
		else
			throw new RuntimeException("register stack overflow");
	}

}
