package suite.jdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.function.BiPredicate;

import org.junit.Test;
import org.objectweb.asm.Type;

import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.Rethrow;

public class FunCreatorTest {

	public interface IntFun {
		public int apply(int i);
	}

	@Test
	public void testBiPredicate() {
		@SuppressWarnings("rawtypes")
		FunCreator<BiPredicate> fc = new FunCreator<>( //
				BiPredicate.class, //
				"test", //
				Type.getDescriptor(boolean.class), //
				Arrays.asList(Type.getDescriptor(Object.class), Type.getDescriptor(Object.class)));
		fc.create(fc.constant(Boolean.TRUE));
		@SuppressWarnings("unchecked")
		BiPredicate<Object, Object> bp = fc.instantiate();
		assertTrue(bp.test("Hello", "world"));
	}

	@Test
	public void testField() {
		IntFun intFun = createIntFun();
		assertEquals(6, intFun.apply(5));
	}

	@Test
	public void testFun() {
		@SuppressWarnings("rawtypes")
		FunCreator<Fun> fc = new FunCreator<>( //
				Fun.class, //
				"apply", //
				Type.getDescriptor(Object.class), //
				Arrays.asList(Type.getDescriptor(Object.class)));
		fc.create(fc.parameter(1));
		@SuppressWarnings("unchecked")
		Fun<Object, Object> fun = fc.instantiate();
		assertEquals("Hello", fun.apply("Hello"));
	}

	private IntFun createIntFun() {
		String fieldName = "j";
		Class<? extends IntFun> clazz = intFun(fieldName).get();
		return Rethrow.reflectiveOperationException(() -> {
			IntFun f = clazz.newInstance();
			clazz.getDeclaredField(fieldName).set(f, 1);
			return f;
		});
	}

	private FunCreator<IntFun> intFun(String fieldName) {
		FunCreator<IntFun> fc = new FunCreator<>( //
				IntFun.class, //
				"apply", //
				Type.getDescriptor(int.class), //
				Arrays.asList(Type.getDescriptor(int.class)), //
				Read.<String, String> empty2() //
						.cons(fieldName, Type.getDescriptor(int.class)) //
						.toMap());
		fc.create(fc.add(fc.field(fieldName), fc.parameter(1)));
		return fc;
	}

}
