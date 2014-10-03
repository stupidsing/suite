package suite.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import suite.lp.Configuration.ProverConfig;

public class MapifyUtilTest {

	private MapifyUtil mapifyUtil = new MapifyUtil(new InspectUtil());

	public interface I {
	}

	public static class Container {
		private List<I> is;
	}

	public static class A implements I {
		private int i = 123;
	}

	public static class B implements I {
		private String s = "test";
	}

	@Test
	public void testMapify() {
		ProverConfig pc0 = new ProverConfig();
		pc0.setRuleSet(null);

		Object map = mapifyUtil.mapify(ProverConfig.class, pc0);
		assertNotNull(map);
		System.out.println(map);

		ProverConfig pc1 = mapifyUtil.unmapify(ProverConfig.class, map);
		System.out.println(pc1);

		assertEquals(pc0, pc1);
		assertTrue(pc0.hashCode() == pc1.hashCode());
	}

	// When mapifying a field with interface type, it would automatically embed
	// object type information (i.e. class name), and un-mapify accordingly.
	@Test
	public void testPolymorphism() {
		A a = new A();
		B b = new B();
		Container object0 = new Container();
		object0.is = Arrays.asList(a, b);

		Object map = mapifyUtil.mapify(Container.class, object0);
		assertNotNull(map);
		System.out.println(map);

		Container object1 = mapifyUtil.unmapify(Container.class, map);
		assertEquals(A.class, object1.is.get(0).getClass());
		assertEquals(B.class, object1.is.get(1).getClass());
		assertEquals(123, ((A) object1.is.get(0)).i);
		assertEquals("test", ((B) object1.is.get(1)).s);
	}

}
