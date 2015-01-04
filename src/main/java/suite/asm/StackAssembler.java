package suite.asm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import suite.Suite;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.util.Rewriter;
import suite.util.FunUtil.Fun;
import suite.util.Pair;

public class StackAssembler extends Assembler {

	private Node stackOperand0 = Atom.of("$0");
	private Node stackOperand1 = Atom.of("$1");
	private Node registers[] = { Atom.of("EAX"), Atom.of("EBX"), Atom.of("ESI") };

	private Fun<Node, Node[]> matchPush = Suite.matcher("R+: .0");
	private Fun<Node, Node[]> matchPop = Suite.matcher("R-: .0");
	private Fun<Node, Node[]> matchTop = Suite.matcher("TOP: .0");

	private Fun<Node, Node[]> matchBegin = Suite.matcher("RBEGIN: ()");
	private Fun<Node, Node[]> matchEnd = Suite.matcher("REND: ()");
	private Fun<Node, Node[]> matchRest = Suite.matcher("RRESTORE: ()");
	private Fun<Node, Node[]> matchSave = Suite.matcher("RSAVE: ()");

	public StackAssembler(int bits) {
		super(bits);
	}

	@Override
	public List<Pair<Reference, Node>> preassemble(List<Pair<Reference, Node>> lnis0) {
		List<Pair<Reference, Node>> lnis1 = new ArrayList<>();
		Deque<Integer> deque = new ArrayDeque<>();
		int sp = 0;

		for (Pair<Reference, Node> lni0 : lnis0) {
			Node node0 = lni0.t1;
			Node m[];

			if ((m = matchRest.apply(node0)) != null)
				for (int r = sp - 1; r >= 0; r--)
					lnis1.add(Pair.of(new Reference(), Suite.substitute("POP .0", getRegister(r))));
			else if ((m = matchSave.apply(node0)) != null)
				for (int r = 0; r < sp; r++)
					lnis1.add(Pair.of(new Reference(), Suite.substitute("PUSH .0", getRegister(r))));
			else {
				Node node1;

				if ((m = matchBegin.apply(node0)) != null) {
					deque.push(sp);
					sp = 0;
					node1 = Atom.NIL;
				} else if ((m = matchEnd.apply(node0)) != null) {
					if (sp == 0)
						sp = deque.pop();
					else
						throw new RuntimeException("Unbalanced register stack in subroutine definition");
					node1 = Atom.NIL;
				} else if ((m = matchPop.apply(node0)) != null)
					node1 = rewrite(sp--, m[0]);
				else if ((m = matchPush.apply(node0)) != null)
					node1 = rewrite(++sp, m[0]);
				else if ((m = matchTop.apply(node0)) != null)
					node1 = rewrite(sp, m[0]);
				else
					node1 = node0;

				lnis1.add(Pair.of(lni0.t0, node1));
			}
		}

		return lnis1;
	}

	private Node rewrite(int sp, Node n) {
		if (sp - 1 >= 0)
			n = new Rewriter(stackOperand0, getRegister(sp - 1)).replace(n);
		if (sp - 2 >= 0)
			n = new Rewriter(stackOperand1, getRegister(sp - 2)).replace(n);
		return n;
	}

	private Node getRegister(int p) {
		if (p < registers.length)
			return registers[p];
		else
			throw new RuntimeException("Register stack overflow");
	}

}
