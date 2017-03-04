package suite.jdk.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.IntSupplier;

import org.apache.bcel.generic.Type;
import org.junit.Test;

import suite.util.FunUtil.Fun;

public class FunCreatorTest {

	@SuppressWarnings("rawtypes")
	private static LambdaInterface<BiPredicate> lambdaClassBiPredicate = LambdaInterface.of(BiPredicate.class, "test");
	@SuppressWarnings("rawtypes")
	private static LambdaInterface<Fun> lambdaClassFun = LambdaInterface.of(Fun.class);
	private static LambdaInterface<IntFun> lambdaClassIntFun = LambdaInterface.of(IntFun.class);
	private static LambdaInterface<IntSupplier> lambdaClassIntSupplier = LambdaInterface.of(IntSupplier.class);

	private Map<String, Object> void_ = Collections.emptyMap();

	public interface IntFun {
		public int apply(int i);
	}

	@Test
	public void testApply0() {
		String fieldName0 = "f0";
		String fieldName1 = "f1";
		FunCreator<IntFun> fc0 = intFun(fieldName0, Type.INT);
		FunCreator<IntFun> fc1 = intFun(fieldName1, Type.getType(IntFun.class));
		IntFun f0 = fc0 //
				.create((i -> fc0.add(fc0.field(fieldName0), i))) //
				.apply(Collections.singletonMap(fieldName0, 1));
		IntFun f1 = fc1 //
				.create(i -> fc1.field(fieldName1).apply(fc1.constant(3))) //
				.apply(Collections.singletonMap(fieldName1, f0));
		assertEquals(4, f1.apply(5));
	}

	@Test
	public void testApply1() {
		FunFactory f = new FunFactory();
		LambdaInstance<IntFun> lambda0 = LambdaInstance.of(lambdaClassIntFun, //
				f.parameter1(i -> f.add(f.constant(1), i)));
		LambdaInstance<IntFun> lambda1 = LambdaInstance.of(lambdaClassIntFun, //
				f.parameter1(i -> f.add(f.constant(1), f.invoke(lambda0, i))));
		assertEquals(2, lambda1.newFun().apply(0));
	}

	@Test
	public void testBiPredicate() {
		@SuppressWarnings("rawtypes")
		FunCreator<BiPredicate> fc = FunCreator.of(lambdaClassBiPredicate);
		@SuppressWarnings("unchecked")
		BiPredicate<Object, Object> bp = fc //
				.create((p, q) -> fc._true()) //
				.apply(void_);
		assertTrue(bp.test("Hello", "world"));
	}

	@Test
	public void testConstant() {
		FunCreator<IntFun> fc = FunCreator.of(lambdaClassIntFun);
		IntFun f = fc //
				.create(i -> fc.constant(1)) //
				.apply(void_);
		assertEquals(1, f.apply(0));
	}

	@Test
	public void testField() {
		String fieldName = "f";
		FunCreator<IntFun> fc = intFun(fieldName, Type.INT);
		int result = fc //
				.create(i -> fc.add(fc.field(fieldName), i)) //
				.apply(Collections.singletonMap(fieldName, 1)) //
				.apply(5);
		assertEquals(6, result);
	}

	@Test
	public void testFun() {
		@SuppressWarnings("rawtypes")
		FunCreator<Fun> fc = FunCreator.of(lambdaClassFun);
		@SuppressWarnings("unchecked")
		Fun<Object, Object> fun = fc.create(o -> o).apply(void_);
		assertEquals("Hello", fun.apply("Hello"));
	}

	@Test
	public void testIf() {
		FunCreator<IntSupplier> fc = FunCreator.of(lambdaClassIntSupplier);
		Object result = fc //
				.create(() -> fc.if_(fc._true(), fc._true(), fc._false())) //
				.apply(void_) //
				.getAsInt();
		assertEquals(1, result);
	}

	@Test
	public void testLocal() {
		FunCreator<IntFun> fc = FunCreator.of(lambdaClassIntFun);
		int result = fc //
				.create(p -> fc.declare(fc.constant(1), l -> fc.add(l, p))) //
				.apply(void_) //
				.apply(3);
		assertEquals(4, result);
	}

	@Test
	public void testObject() {
		IntFun inc = i -> i + 1;
		FunCreator<IntFun> fc = FunCreator.of(lambdaClassIntFun);
		int result = fc //
				.create(i -> fc.object(inc, IntFun.class).invoke("apply", i)) //
				.apply(void_) //
				.apply(2);
		assertEquals(3, result);
	}

	private FunCreator<IntFun> intFun(String fieldName, Type fieldType) {
		return FunCreator.of(lambdaClassIntFun, Collections.singletonMap(fieldName, fieldType));
	}

}
