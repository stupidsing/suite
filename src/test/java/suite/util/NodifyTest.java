package suite.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import suite.lp.Configuration.ProverCfg;
import suite.node.util.Singleton;

public class NodifyTest {

	private Nodify nodify = Singleton.me.nodify;

	public interface I {
	}

	public static class Container {
		private List<I> is;
		private I[] array = { new A(), new B(), };
	}

	public static class A implements I {
		private int i = 123;
		private int[] ints = { 0, 1, 2, };
	}

	public static class B implements I {
		private String s = "test";
	}

	@Test
	public void testMapify() {
		var pc0 = new ProverCfg();
		pc0.setRuleSet(null);

		var node = nodify.nodify(ProverCfg.class, pc0);
		assertNotNull(node);
		System.out.println(node);

		var pc1 = nodify.unnodify(ProverCfg.class, node);
		System.out.println(pc1);

		assertEquals(pc0, pc1);
		assertTrue(pc0.hashCode() == pc1.hashCode());
	}

	// when mapifying a field with interface type, it would automatically embed
	// object type information (i.e. class name), and un-mapify accordingly.
	@Test
	public void testPolymorphism() {
		var a = new A();
		var b = new B();
		var object0 = new Container();
		object0.is = List.of(a, b);

		var node = nodify.nodify(Container.class, object0);
		assertNotNull(node);
		System.out.println(node);

		var object1 = nodify.unnodify(Container.class, node);
		assertEquals(A.class, object1.is.get(0).getClass());
		assertEquals(B.class, object1.is.get(1).getClass());
		assertEquals(123, ((A) object1.is.get(0)).i);
		assertEquals(2, ((A) object1.is.get(0)).ints[2]);
		assertEquals("test", ((B) object1.is.get(1)).s);
		assertEquals(123, ((A) object1.array[0]).i);
		assertEquals(2, ((A) object1.array[0]).ints[2]);
		assertEquals("test", ((B) object1.array[1]).s);
	}

}
