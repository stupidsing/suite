package suite.asm;

import java.util.ArrayList;
import java.util.List;

import suite.Suite;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.util.Rewriter;
import suite.util.Pair;

public class StackAssembler extends Assembler {

	private Node stackOperand = Atom.of("$");
	private Node registers[] = { Atom.of("EAX"), Atom.of("EBX"), Atom.of("ECX"), Atom.of("EDX") };

	public StackAssembler(int bits) {
		super(bits);
	}

	@Override
	public List<Pair<Reference, Node>> preassemble(List<Pair<Reference, Node>> lnis0) {
		List<Pair<Reference, Node>> lnis1 = new ArrayList<>();
		int sp = 0;

		for (Pair<Reference, Node> lni0 : lnis0) {
			Node node0 = lni0.t1;
			Node node1;
			Node m[];
			if ((m = Suite.match("R+ # .0", node0)) != null)
				node1 = new Rewriter(stackOperand, getRegister(sp++)).replace(m[0]);
			else if ((m = Suite.match("R- # .0", node0)) != null)
				node1 = new Rewriter(stackOperand, getRegister(--sp)).replace(m[0]);
			else if ((m = Suite.match("RR # .0", node0)) != null) {
				sp--;
				node1 = Suite.substitute(".0 .1 .2", m[0], getRegister(sp - 1), getRegister(sp));
			} else if ((m = Suite.match("TOP # .0", node0)) != null)
				node1 = new Rewriter(stackOperand, getRegister(sp - 1)).replace(m[0]);
			else
				node1 = node0;
			lnis1.add(Pair.of(lni0.t0, node1));
		}

		return lnis1;
	}

	private Node getRegister(int p) {
		if (p < registers.length)
			return registers[p];
		else
			throw new RuntimeException("Register stack overflow");
	}

}
