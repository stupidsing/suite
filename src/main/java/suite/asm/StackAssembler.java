package suite.asm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import suite.Suite;
import suite.adt.Pair;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.util.TreeRewriter;

public class StackAssembler extends Assembler {

	private Node stackOperand0 = Atom.of("$0");
	private Node stackOperand1 = Atom.of("$1");
	private Node registers[] = { Atom.of("EAX"), Atom.of("EBX"), Atom.of("ESI") };

	private Node push = Atom.of("R+");
	private Node pop = Atom.of("R-");
	private Node begin = Atom.of("RBEGIN");
	private Node end = Atom.of("REND");
	private Node rest = Atom.of("RRESTORE");
	private Node save = Atom.of("RSAVE");

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

			if (node0 == rest)
				for (int r = sp - 1; r >= 0; r--)
					lnis1.add(Pair.of(new Reference(), Suite.substitute("POP .0", getRegister(r))));
			else if (node0 == save)
				for (int r = 0; r < sp; r++)
					lnis1.add(Pair.of(new Reference(), Suite.substitute("PUSH .0", getRegister(r))));
			else {
				Node node1;

				if (node0 == begin) {
					deque.push(sp);
					sp = 0;
					node1 = Atom.NIL;
				} else if (node0 == end) {
					if (sp == 0)
						sp = deque.pop();
					else
						throw new RuntimeException("Unbalanced register stack in subroutine definition");
					node1 = Atom.NIL;
				} else if (node0 == push) {
					sp++;
					node1 = Atom.NIL;
				} else if (node0 == pop) {
					sp--;
					node1 = Atom.NIL;
				} else
					node1 = rewrite(sp, node0);

				lnis1.add(Pair.of(lni0.t0, node1));
			}
		}

		return lnis1;
	}

	private Node rewrite(int sp, Node n) {
		if (sp - 1 >= 0)
			n = new TreeRewriter().replace(stackOperand0, getRegister(sp - 1), n);
		if (sp - 2 >= 0)
			n = new TreeRewriter().replace(stackOperand1, getRegister(sp - 2), n);
		return n;
	}

	private Node getRegister(int p) {
		if (p < registers.length)
			return registers[p];
		else
			throw new RuntimeException("Register stack overflow");
	}

}
