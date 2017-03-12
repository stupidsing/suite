package suite.jdk.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.function.BiPredicate;

import org.apache.bcel.generic.Type;
import org.junit.Test;

import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.lambda.LambdaInstance;
import suite.jdk.lambda.LambdaInterface;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
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
		LambdaInstance<IntFun> lambda0 = LambdaInstance.of(IntFun.class, i -> f.add(f.int_(1), i));
		LambdaInstance<IntFun> lambda1 = LambdaInstance.of(IntFun.class, i -> f.add(f.int_(1), f.invoke(lambda0, i)));
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
		Source<FunExpr> fun = () -> f.declare(f.int_(1),
				one -> f.parameter1(j -> f.add(one, j)).cast(IntFun.class).apply(f.int_(2)));
		assertEquals(3, LambdaInstance.of(IntSource.class, fun).newFun().source());
	}

	@Test
	public void testConstant() {
		Fun<FunExpr, FunExpr> fun = i -> f.int_(1);
		assertEquals(1, LambdaInstance.of(IntSource.class, fun).newFun().source());
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
		Source<FunExpr> fun = () -> f.if_(f._true(), f._true(), f._false());
		assertEquals(1, LambdaInstance.of(IntSource.class, fun).newFun().source());
	}

	@Test
	public void testIndex() {
		int ints[] = { 0, 1, 4, 9, 16, };
		IntFun fun = LambdaInstance.of(IntFun.class, i -> f.object(ints, int[].class).index(i)).newFun();
		assertEquals(9, fun.apply(3));
		assertEquals(16, fun.apply(4));
	}

	@Test
	public void testLocal() {
		Fun<FunExpr, FunExpr> fun = p -> f.declare(f.int_(1), l -> f.add(l, p));
		assertEquals(4, LambdaInstance.of(IntFun.class, fun).newFun().apply(3));
	}

	@Test
	public void testObject() {
		IntFun inc = i -> i + 1;
		Fun<FunExpr, FunExpr> fun = i -> f.object(inc, IntFun.class).invoke("apply", i);
		assertEquals(3, LambdaInstance.of(IntFun.class, fun).newFun().apply(2));
	}

	private FunCreator<IntFun> intFun(String fieldName, Type fieldType) {
		return FunCreator.of(lambdaClassIntFun, To.map(fieldName, fieldType));
	}

}
