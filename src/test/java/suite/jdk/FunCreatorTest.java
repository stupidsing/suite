package suite.jdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.function.BiPredicate;

import org.junit.Test;

import suite.streamlet.Read;
import suite.util.FunUtil.Fun;

public class FunCreatorTest {

	public interface IntFun {
		public int apply(int i);
	}

	@Test
	public void testBiPredicate() {
		@SuppressWarnings("rawtypes")
		FunCreator<BiPredicate> fc = FunCreator.of(BiPredicate.class, "test");
		@SuppressWarnings("unchecked")
		BiPredicate<Object, Object> bp = fc //
				.create(fc.true_()) //
				.apply(new HashMap<>());
		assertTrue(bp.test("Hello", "world"));
	}

	@Test
	public void testField() {
		String fieldName = "f";
		FunCreator<IntFun> fc = intFun(fieldName, int.class);
		int result = fc //
				.create(fc.add(fc.field(fieldName), fc.parameter(1))) //
				.apply(Read.<String, Object> empty2().cons(fieldName, 1).toMap()) //
				.apply(5);
		assertEquals(6, result);
	}

	@Test
	public void testIf() {
		FunCreator<IntFun> fc = FunCreator.of(IntFun.class, "apply");
		int result = fc //
				.create(fc.if_(fc.true_(), fc.true_(), fc.false_())) //
				.apply(new HashMap<>()) //
				.apply(0);
		assertEquals(1, result);
	}

	@Test
	public void testLocal() {
		FunCreator<IntFun> fc = FunCreator.of(IntFun.class, "apply");
		int result = fc //
				.create(fc.local(fc.constant(1), l -> fc.add(l, fc.parameter(1)))) //
				.apply(new HashMap<>()) //
				.apply(3);
		assertEquals(4, result);
	}

	@Test
	public void testFun() {
		@SuppressWarnings("rawtypes")
		FunCreator<Fun> fc = FunCreator.of(Fun.class, "apply");
		@SuppressWarnings("unchecked")
		Fun<Object, Object> fun = fc //
				.create(fc.parameter(1)) //
				.apply(new HashMap<>());
		assertEquals("Hello", fun.apply("Hello"));
	}

	@Test
	public void testInvoke() {
		String fieldName0 = "f0";
		String fieldName1 = "f1";
		FunCreator<IntFun> fc0 = intFun(fieldName0, int.class);
		FunCreator<IntFun> fc1 = intFun(fieldName1, IntFun.class);
		IntFun f0 = fc0 //
				.create(fc0.add(fc0.field(fieldName0), fc0.parameter(1))) //
				.apply(Read.<String, Object> empty2().cons(fieldName0, 1).toMap());
		IntFun f1 = fc1 //
				.create(fc1.field(fieldName1).invoke(fc0, fc1.constant(3))) //
				.apply(Read.<String, Object> empty2().cons(fieldName1, f0).toMap());
		assertEquals(4, f1.apply(5));
	}

	private FunCreator<IntFun> intFun(String fieldName, Class<?> fieldType) {
		return FunCreator.of(IntFun.class, "apply", Read.<String, Class<?>> empty2().cons(fieldName, fieldType).toMap());
	}

}
