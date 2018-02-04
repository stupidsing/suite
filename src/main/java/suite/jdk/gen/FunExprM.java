package suite.jdk.gen;

import java.util.List;
import java.util.Map;

import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import suite.adt.Mutable;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Ints.IntsBuilder;
import suite.util.Util;

/**
 * Functional expressions that can be handled by FunGenerateBytecode.java.
 *
 * @author ywsing
 */
public class FunExprM {

	public static class ArrayFunExpr extends FunExpr {
		public Class<?> clazz;
		public FunExpr[] elements;
	}

	public static class ArrayLengthFunExpr extends FunExpr {
		public FunExpr expr;
	}

	public static class AssignLocalFunExpr extends FunExpr {
		public FunExpr var;
		public FunExpr value;
	}

	public static class BinaryFunExpr extends FunExpr {
		public String op;
		public FunExpr left, right;
	}

	public static class BlockFunExpr extends FunExpr {
		public IntsBuilder breaks;
		public IntsBuilder continues;
		public FunExpr expr;
	}

	public static class BlockBreakFunExpr extends FunExpr {
		public Mutable<BlockFunExpr> block;
	}

	public static class BlockContFunExpr extends FunExpr {
		public Mutable<BlockFunExpr> block;
	}

	public static class CastFunExpr extends FunExpr {
		public Type type;
		public FunExpr expr;
	}

	public static class CheckCastFunExpr extends FunExpr {
		public ReferenceType type;
		public FunExpr expr;
	}

	public static class ConstantFunExpr extends FunExpr {
		public Type type;
		public Object constant; // primitives, class, handles etc.
	}

	public static class FieldStaticFunExpr extends FunExpr {
		public String fieldName;
		public Type fieldType;
	}

	public static class FieldTypeFunExpr_ extends FunExpr {
		public String fieldName;
		public Type fieldType;
		public FunExpr object;
	}

	public static class FieldTypeFunExpr extends FieldTypeFunExpr_ {
	}

	public static class FieldTypeSetFunExpr extends FieldTypeFunExpr_ {
		public FunExpr value;
	}

	public static class IfFunExpr extends FunExpr {
		public FunExpr then, else_;
	}

	public static class If1FunExpr extends IfFunExpr {
		public FunExpr if_;
	}

	public static class If2FunExpr extends IfFunExpr {
		public Obj_Int<Type> opcode;
		public FunExpr left, right;
	}

	public static class IfNonNullFunExpr extends IfFunExpr {
		public FunExpr object;
	}

	public static class IndexFunExpr extends FunExpr {
		public FunExpr array;
		public FunExpr index;
	}

	public static class InstanceOfFunExpr extends FunExpr {
		public FunExpr object;
		public ReferenceType instanceType;
	}

	public static class InvokeMethodFunExpr extends FunExpr {
		public Class<?> clazz;
		public String methodName;
		public FunExpr object;
		public List<FunExpr> parameters;
	}

	public static class LocalFunExpr extends FunExpr {
		public int index;
	}

	public static class NewFunExpr extends FunExpr {
		public String className;
		public Map<String, FunExpr> fieldValues;
		public Class<?> implementationClass, interfaceClass;
	}

	public static class PrintlnFunExpr extends FunExpr {
		public FunExpr expression;
	}

	public static class ProfileFunExpr extends FunExpr {
		public String counterFieldName = "p" + Util.temp();
		public FunExpr do_;
	}

	public static class SeqFunExpr extends FunExpr {
		public FunExpr left, right;
	}

	public static class VoidFunExpr extends FunExpr {
	}

}
