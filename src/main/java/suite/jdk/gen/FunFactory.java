package suite.jdk.gen;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import org.apache.bcel.Const;
import org.apache.bcel.generic.Type;

import suite.jdk.gen.FunExpression.BinaryFunExpr;
import suite.jdk.gen.FunExpression.ConstantFunExpr;
import suite.jdk.gen.FunExpression.Declare1ParameterFunExpr;
import suite.jdk.gen.FunExpression.Declare2ParameterFunExpr;
import suite.jdk.gen.FunExpression.DeclareLocalFunExpr;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunExpression.If1FunExpr;
import suite.jdk.gen.FunExpression.If2FunExpr;
import suite.jdk.gen.FunExpression.InjectFunExpr;
import suite.jdk.gen.FunExpression.InvokeFunExpr;
import suite.jdk.gen.FunExpression.LocalFunExpr;
import suite.jdk.gen.FunExpression.ObjectFunExpr;
import suite.jdk.gen.FunExpression.PlaceholderFunExpr;
import suite.jdk.gen.FunExpression.SeqFunExpr;
import suite.util.FunUtil.Fun;

public class FunFactory {

	public static final FunExpression fe = new FunExpression();

	public FunExpr _true() {
		return constant(1);
	}

	public FunExpr _false() {
		return constant(0);
	}

	public FunExpr add(FunExpr e0, FunExpr e1) {
		return bi("+", e0, e1);
	}

	public FunExpr bi(String op, FunExpr e0, FunExpr e1) {
		BinaryFunExpr expr = fe.new BinaryFunExpr();
		expr.op = op;
		expr.left = e0;
		expr.right = e1;
		return expr;
	}

	public FunExpr constant(int i) {
		ConstantFunExpr expr = fe.new ConstantFunExpr();
		expr.type = Type.INT;
		expr.constant = i;
		return expr;
	}

	public FunExpr declare(FunExpr value, Fun<FunExpr, FunExpr> doFun) {
		PlaceholderFunExpr var = fe.new PlaceholderFunExpr();

		DeclareLocalFunExpr expr = fe.new DeclareLocalFunExpr();
		expr.var = var;
		expr.value = value;
		expr.do_ = doFun.apply(var);
		return expr;
	}

	public FunExpr if_(FunExpr if_, FunExpr then_, FunExpr else_) {
		If1FunExpr expr = fe.new If1FunExpr();
		expr.if_ = if_;
		expr.then = then_;
		expr.else_ = else_;
		return expr;
	}

	public FunExpr ifeq(FunExpr left, FunExpr right, FunExpr then_, FunExpr else_) {
		If2FunExpr expr = fe.new If2FunExpr();
		expr.opcode = t -> !Objects.equals(t, Type.INT) ? Const.IF_ACMPNE : Const.IF_ICMPNE;
		expr.left = left;
		expr.right = right;
		expr.then = then_;
		expr.else_ = else_;
		return expr;
	}

	public FunExpr ifInstance(Class<?> clazz, FunExpr object, Fun<FunExpr, FunExpr> then_, FunExpr else_) {
		return if_(object.instanceOf(clazz), declare(object.checkCast(clazz), o_ -> then_.apply(o_)), else_);
	}

	public FunExpr inject(String field) {
		InjectFunExpr expr = fe.new InjectFunExpr();
		expr.field = field;
		return expr;
	}

	public FunExpr invoke(LambdaImplementation<?> lambda, Map<String, Object> fieldValues, FunExpr parameter) {
		InvokeFunExpr expr = fe.new InvokeFunExpr();
		expr.lambda = lambda;
		expr.fieldValues = fieldValues;
		expr.parameters = Arrays.asList(parameter);
		return expr;
	}

	public FunExpr local(int number, Class<?> clazz) {
		return local(number, Type.getType(clazz));
	}

	public FunExpr local(int number, Type type) { // 0 means this
		LocalFunExpr expr = fe.new LocalFunExpr();
		expr.type = type;
		expr.index = number;
		return expr;
	}

	public FunExpr object(Object object) {
		return object_(object, object.getClass());
	}

	public <C, T extends C> FunExpr object(T object, Class<C> clazz) {
		return object_(object, clazz);
	}

	public FunExpr object_(Object object, Class<?> clazz) {
		ObjectFunExpr expr = fe.new ObjectFunExpr();
		expr.clazz = clazz;
		expr.object = object;
		return expr;
	}

	public FunExpr parameter(Fun<FunExpr, FunExpr> doFun) {
		PlaceholderFunExpr parameter = fe.new PlaceholderFunExpr();

		Declare1ParameterFunExpr expr = fe.new Declare1ParameterFunExpr();
		expr.parameter = parameter;
		expr.do_ = doFun.apply(parameter);
		return expr;
	}

	public FunExpr parameter2(BiFunction<FunExpr, FunExpr, FunExpr> doFun) {
		PlaceholderFunExpr p0 = fe.new PlaceholderFunExpr();
		PlaceholderFunExpr p1 = fe.new PlaceholderFunExpr();

		Declare2ParameterFunExpr expr = fe.new Declare2ParameterFunExpr();
		expr.p0 = p0;
		expr.p1 = p1;
		expr.do_ = doFun.apply(p0, p1);
		return expr;
	}

	public FunExpr seq(FunExpr e0, FunExpr e1) {
		SeqFunExpr expr = fe.new SeqFunExpr();
		expr.left = e0;
		expr.right = e1;
		return expr;
	}

}
