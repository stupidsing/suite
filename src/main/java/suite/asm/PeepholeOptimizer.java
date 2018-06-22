package suite.asm;

import java.util.ArrayList;
import java.util.List;

import suite.BindArrayUtil.Pattern;
import suite.Suite;
import suite.adt.pair.FixieArray;
import suite.adt.pair.Pair;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.util.TreeUtil;

public class PeepholeOptimizer {

	private Pattern ADDI__ = Suite.pattern("ADDI (.0, .1)");
	private Pattern MOV___ = Suite.pattern("MOV (.0, .1)");

	public List<Pair<Reference, Node>> optimize(List<Pair<Reference, Node>> lnis0) {
		var lnis1 = new ArrayList<Pair<Reference, Node>>();

		for (var lni0 : lnis0) {
			var node0 = lni0.t1;
			Node node1;
			Node[] m;

			if ((m = ADDI__.match(node0)) != null)
				node1 = FixieArray.of(m).map((m0, m1) -> {
					var i = TreeUtil.evaluate(m1);
					if (i == 1)
						return Suite.substitute("INC .0", m0);
					else if (i == -1)
						return Suite.substitute("DEC .0", m0);
					else if (0 < i)
						return Suite.substitute("ADD (.0, .1)", m0, Int.of(i));
					else if (i < 0)
						return Suite.substitute("SUB (.0, .1)", m0, Int.of(-i));
					else
						return Atom.NIL;
				});
			else if ((m = MOV___.match(node0)) != null)
				node1 = FixieArray.of(m).map((m0, m1) -> {
					if (m0 == m1)
						return Atom.NIL;
					else if (m0 instanceof Atom && m1 instanceof Int && Int.num(m1) == 0)
						return Suite.substitute("XOR (.0, .0)", m0);
					else
						return Suite.substitute("MOV (.0, .1)", m0, m1);
				});
			else
				node1 = node0;

			lnis1.add(Pair.of(lni0.t0, node1));
		}

		return lnis1;
	}

}
