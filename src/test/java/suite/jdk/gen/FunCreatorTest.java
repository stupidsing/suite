package suite.jdk.gen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.function.BiPredicate;

import org.apache.bcel.generic.Type;
import org.junit.Test;

import suite.Suite;
import suite.inspect.Dump;
import suite.jdk.gen.FunExprM.PrintlnFunExpr;
import suite.jdk.gen.FunExprM.ProfileFunExpr;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.lambda.LambdaInstance;
import suite.jdk.lambda.LambdaInterface;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.primitive.Flt_Flt;
import suite.primitive.IntPrimitives.IntSource;
import suite.primitive.Int_Int;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil.Source;

public class FunCreatorTest {

	private static FunFactory f = new FunFactory();
	private static LambdaInterface<Int_Int> lambdaClassIntIntFun = LambdaInterface.of(Int_Int.class);

	private Map<String, Object> void_ = Map.ofEntries();

	@Test
	public void testApply0() {
		var fieldName0 = "f0";
		var fieldName1 = "f1";
		var fc0 = intFun(fieldName0, Type.INT);
		var fc1 = intFun(fieldName1, Type.getType(Int_Int.class));
		var f0 = fc0 //
				.create((i -> f.add(fc0.field(fieldName0), i))) //
				.apply(Map.of(fieldName0, 1));
		var f1 = fc1 //
				.create(i -> fc1.field(fieldName1).apply(f.int_(3))) //
				.apply(Map.of(fieldName1, f0));
		assertEquals(4, f1.apply(5));
	}

	@Test
	public void testApply1() {
		var lambda0 = LambdaInstance.of(Int_Int.class, i -> f.add(f.int_(1), i));
		var lambda1 = LambdaInstance.of(Int_Int.class, i -> f.add(f.int_(1), lambda0.invoke(i)));
		assertEquals(2, lambda1.newFun().apply(0));
	}

	@Test
	public void testArray() {
		var lambda = LambdaInstance.of(Int_Int.class, i -> f.array(int.class, f.int_(1), f.int_(0)).index(i));
		assertEquals(1, lambda.newFun().apply(0));
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
				one -> f.parameter1(j -> f.add(one, j)).cast_(Int_Int.class).apply(f.int_(2)));
		assertEquals(3, LambdaInstance.of(IntSource.class, fun).newFun().source());
	}

	@Test
	public void testConstant() {
		Iterate<FunExpr> fun = i -> f.int_(1);
		assertEquals(1, LambdaInstance.of(IntSource.class, fun).newFun().source());
	}

	@Test
	public void testExpression() {
		var N1 = Int.of(1);
		@SuppressWarnings({ "rawtypes", "unchecked" })
		FunCreator<Source<Node>> fc = (FunCreator) FunCreator.of(Source.class);
		assertEquals(Suite.parse("1"), fc.create(() -> f.object(N1)).apply(void_).source());
		assertEquals(Suite.parse("1 + 1"), fc.create(() -> f //
				.invokeStatic(Tree.class, "of", //
						f.object(TermOp.PLUS__), //
						f.object(N1).cast_(Node.class), //
						f.object(N1).cast_(Node.class))) //
				.apply(void_) //
				.source());
	}

	@Test
	public void testField() {
		var fieldName = "f";
		var fc = intFun(fieldName, Type.INT);
		var result = fc //
				.create(i -> f.add(fc.field(fieldName), i)) //
				.apply(Map.of(fieldName, 1)) //
				.apply(5);
		assertEquals(6, result);
	}

	@Test
	public void testFloat() {
		var lambda0 = LambdaInstance.of(Flt_Flt.class, i -> f.add(f.float_(1), i));
		var lambda1 = LambdaInstance.of(Flt_Flt.class, i -> f.add(f.float_(1), lambda0.invoke(i)));
		assertTrue(lambda1.newFun().apply(0) == 2f);
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
		int[] ints = { 0, 1, 4, 9, 16, };
		var fun = LambdaInstance.of(Int_Int.class, i -> f.object(ints).index(i)).newFun();
		assertEquals(9, fun.apply(3));
		assertEquals(16, fun.apply(4));
	}

	@Test
	public void testLocal() {
		Iterate<FunExpr> fun = p -> f.declare(f.int_(1), l -> f.add(l, p));
		assertEquals(4, LambdaInstance.of(Int_Int.class, fun).newFun().apply(3));
	}

	@Test
	public void testNew() {
		@SuppressWarnings({ "rawtypes", "unchecked" })
		FunCreator<Source<Node>> fc = (FunCreator) FunCreator.of(Source.class);
		assertTrue(fc.create(() -> f.new_(Reference.class)).apply(void_).source() instanceof Reference);
	}

	@Test
	public void testObject() {
		Int_Int inc = i -> i + 1;
		Iterate<FunExpr> fun = i -> f.object(inc).invoke("apply", i);
		assertEquals(3, LambdaInstance.of(Int_Int.class, fun).newFun().apply(2));
	}

	@Test
	public void testProfile() {
		Iterate<FunExpr> fun = i -> (ProfileFunExpr) f.profile(f.int_(1));
		var instance = LambdaInstance.of(IntSource.class, fun).newFun();
		assertEquals(1, instance.source());
		Dump.details(instance);
	}

	@Test
	public void testRunnable() {
		var pfe = new PrintlnFunExpr();
		pfe.expression = f.object(1).cast_(String.class);

		var fc = FunCreator.of(Runnable.class);
		fc.create(() -> pfe).apply(void_).run();
	}

	private FunCreator<Int_Int> intFun(String fieldName, Type fieldType) {
		return FunCreator.of(lambdaClassIntIntFun, Map.of(fieldName, fieldType));
	}

}
