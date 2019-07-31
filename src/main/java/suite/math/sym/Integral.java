package suite.math.sym;

import primal.adt.Opt;
import primal.adt.Pair;
import suite.math.sym.Sym.Ring;
import suite.node.Int;
import suite.node.Node;

public class Integral {

	private Express ex = new Express();

	public Ring<Integer> ring = new Ring<>( //
			0, //
			1, //
			(a, b) -> a + b, //
			a -> -a, //
			(a, b) -> a * b);

	public Opt<Integer> parse(Node node) {
		return node instanceof Int ? Opt.of(Int.num(node)) : Opt.none();
	}

	public Node format(int a) {
		return ex.intOf(a);
	}

	public Pair<Integer, Integer> divMod(int a, int b) {
		var div = a / b;
		var mod = a % b;
		return 0 <= mod ? Pair.of(div, mod) : Pair.of(div - 1, mod + b);
	}

	public int sign(int a) {
		return Integer.compare(a, 0);
	}

}
