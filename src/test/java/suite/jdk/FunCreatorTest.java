package suite.jdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
		FunCreator<BiPredicate> fc = FunCreator.of(BiPredicate.class, "test");
		fc.create(fc.constant(Boolean.TRUE));
		@SuppressWarnings("unchecked")
		BiPredicate<Object, Object> bp = fc.instantiate();
		assertTrue(bp.test("Hello", "world"));
	}

	@Test
	public void testField() {
		String fieldName = "f";
		FunCreator<IntFun> fc = intFun(fieldName, Type.getDescriptor(int.class));
		fc.create(fc.add(fc.field(fieldName), fc.parameter(1)));
		IntFun intFun = instantiate(fc, fieldName, 1);
		assertEquals(6, intFun.apply(5));
	}

	@Test
	public void testFun() {
		@SuppressWarnings("rawtypes")
		FunCreator<Fun> fc = FunCreator.of(Fun.class, "apply");
		fc.create(fc.parameter(1));
		@SuppressWarnings("unchecked")
		Fun<Object, Object> fun = fc.instantiate();
		assertEquals("Hello", fun.apply("Hello"));
	}

	@Test
	public void testInvoke() {
		String fieldName0 = "f0";
		String fieldName1 = "f1";
		FunCreator<IntFun> fc0 = intFun(fieldName0, Type.getDescriptor(int.class));
		FunCreator<IntFun> fc1 = intFun(fieldName1, Type.getDescriptor(IntFun.class));
		fc0.create(fc0.add(fc0.field(fieldName0), fc0.parameter(1)));
		fc1.create(fc1.field(fieldName1).invoke(fc0, fc1.constant(3)));
		IntFun f0 = instantiate(fc0, fieldName0, 1);
		IntFun f1 = instantiate(fc1, fieldName1, f0);
		assertEquals(4, f1.apply(5));
	}

	private FunCreator<IntFun> intFun(String fieldName, String fieldType) {
		return FunCreator.of(IntFun.class, "apply", Read.<String, String> empty2().cons(fieldName, fieldType).toMap());
	}

	private IntFun instantiate(FunCreator<IntFun> fc, String fieldName, Object fieldValue) {
		return Rethrow.reflectiveOperationException(() -> {
			Class<? extends IntFun> clazz = fc.get();
			IntFun f = clazz.newInstance();
			clazz.getDeclaredField(fieldName).set(f, fieldValue);
			return f;
		});
	}

}
