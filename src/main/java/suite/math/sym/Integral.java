package suite.math.sym;

import suite.math.sym.Sym.Ring;

public class Integral {

	public Ring<Integer> ring = new Ring<>( //
			0, //
			1, //
			(a, b) -> a + b, //
			a -> -a, //
			(a, b) -> a * b);

}
