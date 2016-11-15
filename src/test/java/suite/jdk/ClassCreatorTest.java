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

public class ClassCreatorTest {

	public interface IntFun {
		public int apply(int i);
	}

	@Test
	public void testBiPredicate() {
		@SuppressWarnings("rawtypes")
		ClassCreator<BiPredicate> cc = new ClassCreator<>( //
				BiPredicate.class, //
				"test", //
				Type.getDescriptor(boolean.class), //
				Arrays.asList(Type.getDescriptor(Object.class), Type.getDescriptor(Object.class)));
		@SuppressWarnings("unchecked")
		BiPredicate<Object, Object> bp = cc.create(cc.constant(Boolean.TRUE));
		assertTrue(bp.test("Hello", "world"));
	}

	@Test
	public void testField() {
		String fieldName = "j";
		ClassCreator<IntFun> cc = new ClassCreator<>( //
				IntFun.class, //
				"apply", //
				Type.getDescriptor(int.class), //
				Arrays.asList(Type.getDescriptor(int.class)), //
				Read.<String, String> empty2() //
						.cons(fieldName, Type.getDescriptor(int.class)) //
						.toMap());
		Class<? extends IntFun> clazz = cc.clazz(cc.add(cc.field(fieldName), cc.parameter(1)));
		IntFun intFun = Rethrow.reflectiveOperationException(() -> {
			IntFun f = clazz.newInstance();
			clazz.getDeclaredField(fieldName).set(f, 1);
			return f;
		});
		assertEquals(6, intFun.apply(5));
	}

	@Test
	public void testFun() {
		@SuppressWarnings("rawtypes")
		ClassCreator<Fun> cc = new ClassCreator<>( //
				Fun.class, //
				"apply", //
				Type.getDescriptor(Object.class), //
				Arrays.asList(Type.getDescriptor(Object.class)));
		@SuppressWarnings("unchecked")
		Fun<Object, Object> fun = cc.create(cc.parameter(1));
		assertEquals("Hello", fun.apply("Hello"));
	}

}
