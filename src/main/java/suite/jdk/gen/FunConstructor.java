package suite.jdk.gen;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import suite.jdk.gen.FunExpression.BinaryFunExpr;
import suite.jdk.gen.FunExpression.ConstantFunExpr;
import suite.jdk.gen.FunExpression.Declare1ParameterFunExpr;
import suite.jdk.gen.FunExpression.Declare2ParameterFunExpr;
import suite.jdk.gen.FunExpression.DeclareLocalFunExpr;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunExpression.If1FunExpr;
import suite.jdk.gen.FunExpression.If2FunExpr;
import suite.jdk.gen.FunExpression.LocalFunExpr;
import suite.jdk.gen.FunExpression.SeqFunExpr;
import suite.util.FunUtil.Fun;

public class FunConstructor implements Opcodes {

	public final FunExpression fe = new FunExpression();

	public FunExpr _true() {
		return constant(1);
	}

	public FunExpr _false() {
		return constant(0);
	}

	public FunExpr add(FunExpr e0, FunExpr e1) {
		return bi(e0, e1, type -> TypeHelper.choose(type, 0, DADD, FADD, IADD, LADD));
	}

	public FunExpr bi(FunExpr e0, FunExpr e1, ToIntFunction<Type> opcode) {
		BinaryFunExpr expr = fe.new BinaryFunExpr();
		expr.opcode = opcode;
		expr.left = e0;
		expr.right = e1;
		return expr;
	}

	public FunExpr constant(int i) {
		ConstantFunExpr expr = fe.new ConstantFunExpr();
		expr.type = Type.INT_TYPE;
		expr.constant = i;
		return expr;
	}

	public FunExpr declare(FunExpr value, Fun<FunExpr, FunExpr> doFun) {
		DeclareLocalFunExpr expr = fe.new DeclareLocalFunExpr();
		expr.value = value;
		expr.doFun = doFun;
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
		expr.opcode = t -> !Objects.equals(t, Type.INT_TYPE) ? Opcodes.IF_ACMPNE : Opcodes.IF_ICMPNE;
		expr.left = left;
		expr.right = right;
		expr.then = then_;
		expr.else_ = else_;
		return expr;
	}

	public FunExpr ifInstance(Class<?> clazz, FunExpr object, Fun<FunExpr, FunExpr> then_, FunExpr else_) {
		return if_(object.instanceOf(clazz), declare(object.checkCast(clazz), o_ -> then_.apply(o_)), else_);
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

	public FunExpr parameter(Fun<FunExpr, FunExpr> doFun) {
		Declare1ParameterFunExpr expr = fe.new Declare1ParameterFunExpr();
		expr.doFun = doFun;
		return expr;
	}

	public FunExpr parameter2(BiFunction<FunExpr, FunExpr, FunExpr> doFun) {
		Declare2ParameterFunExpr expr = fe.new Declare2ParameterFunExpr();
		expr.doFun = doFun;
		return expr;
	}

	public FunExpr seq(FunExpr e0, FunExpr e1) {
		SeqFunExpr expr = fe.new SeqFunExpr();
		expr.left = e0;
		expr.right = e1;
		return expr;
	}

}
