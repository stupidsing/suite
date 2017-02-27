package suite.jdk.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.function.BiPredicate;

import org.apache.bcel.generic.Type;
import org.junit.Test;

import suite.util.FunUtil.Fun;

public class FunCreatorTest {

	@SuppressWarnings("rawtypes")
	private static LambdaInterface<BiPredicate> lambdaClassBiPredicate = LambdaInterface.of(BiPredicate.class, "test");
	@SuppressWarnings("rawtypes")
	private static LambdaInterface<Fun> lambdaClassFun = LambdaInterface.of(Fun.class);
	private static LambdaInterface<IntFun> lambdaClassIntFun = LambdaInterface.of(IntFun.class);

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
				.create(fc0.parameter(i -> fc0.add(fc0.field(fieldName0), i))) //
				.apply(Collections.singletonMap(fieldName0, 1));
		IntFun f1 = fc1 //
				.create(fc1.field(fieldName1).apply(fc1.constant(3))) //
				.apply(Collections.singletonMap(fieldName1, f0));
		assertEquals(4, f1.apply(5));
	}

	@Test
	public void testApply1() {
		FunFactory f = new FunFactory();

		LambdaImplementation<IntFun> lambda0 = LambdaImplementation.of( //
				lambdaClassIntFun, //
				Collections.emptyMap(), //
				f.parameter(i -> f.add(f.constant(1), i)));

		LambdaImplementation<IntFun> lambda1 = LambdaImplementation.of( //
				lambdaClassIntFun, //
				Collections.emptyMap(), //
				f.parameter(i -> f.add(f.constant(1), f.invoke(lambda0, void_, i))));

		assertEquals(2, lambda1.newFun(void_).apply(0));
	}

	@Test
	public void testBiPredicate() {
		@SuppressWarnings("rawtypes")
		FunCreator<BiPredicate> fc = FunCreator.of(lambdaClassBiPredicate);
		@SuppressWarnings("unchecked")
		BiPredicate<Object, Object> bp = fc //
				.create(fc._true()) //
				.apply(void_);
		assertTrue(bp.test("Hello", "world"));
	}

	@Test
	public void testField() {
		String fieldName = "f";
		FunCreator<IntFun> fc = intFun(fieldName, Type.INT);
		int result = fc //
				.create(fc.parameter(i -> fc.add(fc.field(fieldName), i))) //
				.apply(Collections.singletonMap(fieldName, 1)) //
				.apply(5);
		assertEquals(6, result);
	}

	@Test
	public void testFun() {
		@SuppressWarnings("rawtypes")
		FunCreator<Fun> fc = FunCreator.of(lambdaClassFun);
		@SuppressWarnings("unchecked")
		Fun<Object, Object> fun = fc.create(fc.parameter(o -> o)).apply(void_);
		assertEquals("Hello", fun.apply("Hello"));
	}

	@Test
	public void testIf() {
		FunCreator<IntFun> fc = FunCreator.of(lambdaClassIntFun);
		int result = fc //
				.create(fc.if_(fc._true(), fc._true(), fc._false())) //
				.apply(void_) //
				.apply(0);
		assertEquals(1, result);
	}

	@Test
	public void testLocal() {
		FunCreator<IntFun> fc = FunCreator.of(lambdaClassIntFun);
		int result = fc //
				.create(fc.parameter(p -> fc.declare(fc.constant(1), l -> fc.add(l, p)))) //
				.apply(void_) //
				.apply(3);
		assertEquals(4, result);
	}

	@Test
	public void testObject() {
		IntFun inc = i -> i + 1;
		FunCreator<IntFun> fc = FunCreator.of(lambdaClassIntFun);
		int result = fc //
				.create(fc.parameter(i -> fc.object(inc, IntFun.class).invoke("apply", i))) //
				.apply(void_) //
				.apply(2);
		assertEquals(3, result);
	}

	private FunCreator<IntFun> intFun(String fieldName, Type fieldType) {
		return FunCreator.of(lambdaClassIntFun, Collections.singletonMap(fieldName, fieldType));
	}

}
