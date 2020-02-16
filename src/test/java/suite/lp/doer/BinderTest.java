package suite.lp.doer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import suite.node.Atom;
import suite.node.Dict;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;

public class BinderTest {

	@Test
	public void testOkay() {
		assertTrue(bind(Atom.NIL, Atom.NIL));

		var key0 = Atom.of("k0");
		var key1 = Atom.of("k1");
		var ref0 = new Reference();
		var ref1 = new Reference();

		var map0 = new HashMap<Node, Reference>();
		map0.put(key0, ref0);

		var map1 = new HashMap<Node, Reference>();
		map1.put(key1, ref1);

		assertTrue(bind(Dict.of(map0), Dict.of(map1)));
		System.out.println(map0);
		System.out.println(map1);

		bind(ref0, Atom.NIL);
		assertEquals(Atom.NIL, map1.get(key0).finalNode());
	}

	@Test
	public void testFail() {
		assertFalse(bind(Atom.NIL, Int.of(0)));

		var key = Atom.of("key");

		var map0 = new HashMap<Node, Reference>();
		map0.put(key, Reference.of(Atom.of("A")));

		var map1 = new HashMap<Node, Reference>();
		map1.put(key, Reference.of(Atom.of("B")));

		assertFalse(bind(Dict.of(map0), Dict.of(map1)));
	}

	private boolean bind(Node n0, Node n1) {
		return Binder.bind(n0, n1);
	}

}
