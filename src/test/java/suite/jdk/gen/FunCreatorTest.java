package suite.jdk.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.function.BiPredicate;

import org.apache.bcel.generic.Type;
import org.junit.Test;

import suite.jdk.lambda.LambdaInstance;
import suite.jdk.lambda.LambdaInterface;
import suite.util.FunUtil.Fun;
import suite.util.To;

public class FunCreatorTest {

	private static FunFactory f = new FunFactory();
	private static LambdaInterface<IntFun> lambdaClassIntFun = LambdaInterface.of(IntFun.class);

	private Map<String, Object> void_ = Collections.emptyMap();

	public interface IntFun {
		public int apply(int i);
	}

	public interface IntSource {
		public int source();
	}

	@Test
	public void testApply0() {
		String fieldName0 = "f0";
		String fieldName1 = "f1";
		FunCreator<IntFun> fc0 = intFun(fieldName0, Type.INT);
		FunCreator<IntFun> fc1 = intFun(fieldName1, Type.getType(IntFun.class));
		IntFun f0 = fc0 //
				.create((i -> f.add(fc0.field(fieldName0), i))) //
				.apply(To.map(fieldName0, 1));
		IntFun f1 = fc1 //
				.create(i -> fc1.field(fieldName1).apply(f.int_(3))) //
				.apply(To.map(fieldName1, f0));
		assertEquals(4, f1.apply(5));
	}

	@Test
	public void testApply1() {
		LambdaInstance<IntFun> lambda0 = LambdaInstance.of(lambdaClassIntFun, //
				f.parameter1(i -> f.add(f.int_(1), i)));
		LambdaInstance<IntFun> lambda1 = LambdaInstance.of(lambdaClassIntFun, //
				f.parameter1(i -> f.add(f.int_(1), f.invoke(lambda0, i))));
		assertEquals(2, lambda1.newFun().apply(0));
	}

	@Test
	public void testBiPredicate() {
		@SuppressWarnings("rawtypes")
		FunCreator<BiPredicate> fc = FunCreator.of(BiPredicate.class);
		@SuppressWarnings("unchecked")
		BiPredicate<Object, Object> bp = fc.create((p, q) -> f._true()).apply(void_);
		assertTrue(bp.test("Hello", "world"));
	}

	@Test
	public void testClosure() {
		FunCreator<IntSource> fc = FunCreator.of(IntSource.class);
		IntSource source = fc //
				.create(() -> f.declare(f.int_(1), one -> f.parameter1(j -> f.add(one, j)).cast(IntFun.class).apply(f.int_(2)))) //
				.apply(void_);
		assertEquals(3, source.source());
	}

	@Test
	public void testConstant() {
		FunCreator<IntFun> fc = FunCreator.of(IntFun.class);
		IntFun fun = fc.create(i -> f.int_(1)).apply(void_);
		assertEquals(1, fun.apply(0));
	}

	@Test
	public void testField() {
		String fieldName = "f";
		FunCreator<IntFun> fc = intFun(fieldName, Type.INT);
		int result = fc //
				.create(i -> f.add(fc.field(fieldName), i)) //
				.apply(To.map(fieldName, 1)) //
				.apply(5);
		assertEquals(6, result);
	}

	@Test
	public void testFun() {
		@SuppressWarnings("rawtypes")
		FunCreator<Fun> fc = FunCreator.of(Fun.class);
		@SuppressWarnings("unchecked")
		Fun<Object, Object> fun = fc.create(o -> o).apply(void_);
		assertEquals("Hello", fun.apply("Hello"));
	}

	@Test
	public void testIf() {
		FunCreator<IntSource> fc = FunCreator.of(IntSource.class);
		Object result = fc //
				.create(() -> f.if_(f._true(), f._true(), f._false())) //
				.apply(void_) //
				.source();
		assertEquals(1, result);
	}

	@Test
	public void testIndex() {
		FunCreator<IntFun> fc = FunCreator.of(IntFun.class);
		int ints[] = { 0, 1, 4, 9, 16, };
		IntFun fun = fc //
				.create(i -> f.object(ints, int[].class).index(i)) //
				.apply(void_);
		assertEquals(9, fun.apply(3));
		assertEquals(16, fun.apply(4));
	}

	@Test
	public void testLocal() {
		FunCreator<IntFun> fc = FunCreator.of(IntFun.class);
		int result = fc //
				.create(p -> f.declare(f.int_(1), l -> f.add(l, p))) //
				.apply(void_) //
				.apply(3);
		assertEquals(4, result);
	}

	@Test
	public void testObject() {
		IntFun inc = i -> i + 1;
		FunCreator<IntFun> fc = FunCreator.of(IntFun.class);
		int result = fc //
				.create(i -> f.object(inc, IntFun.class).invoke("apply", i)) //
				.apply(void_) //
				.apply(2);
		assertEquals(3, result);
	}

	private FunCreator<IntFun> intFun(String fieldName, Type fieldType) {
		return FunCreator.of(lambdaClassIntFun, To.map(fieldName, fieldType));
	}

}
