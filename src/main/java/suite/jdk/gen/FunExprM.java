package suite.jdk.gen;

import java.util.List;
import java.util.Map;

import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import primal.Verbs.Get;
import primal.adt.Fixie_.FixieFun0;
import primal.adt.Fixie_.FixieFun1;
import primal.adt.Fixie_.FixieFun2;
import primal.adt.Fixie_.FixieFun3;
import primal.adt.Fixie_.FixieFun4;
import primal.adt.Mutable;
import primal.primitive.IntPrim.Obj_Int;
import primal.primitive.adt.Ints.IntsBuilder;
import suite.jdk.gen.FunExpression.FunExpr;

/**
 * Functional expressions that can be handled by FunGenerateBytecode.java.
 *
 * @author ywsing
 */
public class FunExprM {

	public static class ArrayFunExpr extends FunExpr {
		public Class<?> clazz;
		public FunExpr[] elements;

		public <R> R apply(FixieFun2<Class<?>, FunExpr[], R> fun) {
			return fun.apply(clazz, elements);
		}
	}

	public static class ArrayLengthFunExpr extends FunExpr {
		public FunExpr expr;

		public <R> R apply(FixieFun1<FunExpr, R> fun) {
			return fun.apply(expr);
		}
	}

	public static class AssignLocalFunExpr extends FunExpr {
		public FunExpr var;
		public FunExpr value;

		public <R> R apply(FixieFun2<FunExpr, FunExpr, R> fun) {
			return fun.apply(var, value);
		}
	}

	public static class BinaryFunExpr extends FunExpr {
		public String op;
		public FunExpr left, right;

		public <R> R apply(FixieFun3<String, FunExpr, FunExpr, R> fun) {
			return fun.apply(op, left, right);
		}
	}

	public static class BlockFunExpr extends FunExpr {
		public IntsBuilder breaks;
		public IntsBuilder continues;
		public FunExpr expr;

		public <R> R apply(FixieFun3<IntsBuilder, IntsBuilder, FunExpr, R> fun) {
			return fun.apply(breaks, continues, expr);
		}
	}

	public static class BlockBreakFunExpr extends FunExpr {
		public Mutable<BlockFunExpr> block;

		public <R> R apply(FixieFun1<Mutable<BlockFunExpr>, R> fun) {
			return fun.apply(block);
		}
	}

	public static class BlockContFunExpr extends FunExpr {
		public Mutable<BlockFunExpr> block;

		public <R> R apply(FixieFun1<Mutable<BlockFunExpr>, R> fun) {
			return fun.apply(block);
		}
	}

	public static class CastFunExpr extends FunExpr {
		public Type type;
		public FunExpr expr;

		public <R> R apply(FixieFun2<Type, FunExpr, R> fun) {
			return fun.apply(type, expr);
		}
	}

	public static class CheckCastFunExpr extends FunExpr {
		public ReferenceType type;
		public FunExpr expr;

		public <R> R apply(FixieFun2<ReferenceType, FunExpr, R> fun) {
			return fun.apply(type, expr);
		}
	}

	public static class ConstantFunExpr extends FunExpr {
		public Type type;
		public Object constant; // primitives, class, handles etc.

		public <R> R apply(FixieFun2<Type, Object, R> fun) {
			return fun.apply(type, constant);
		}
	}

	public static class FieldStaticFunExpr extends FunExpr {
		public String fieldName;
		public Type fieldType;

		public <R> R apply(FixieFun2<String, Type, R> fun) {
			return fun.apply(fieldName, fieldType);
		}
	}

	public static class FieldTypeFunExpr_ extends FunExpr {
		public String fieldName;
		public Type fieldType;
		public FunExpr object;

		public <R> R apply(FixieFun3<String, Type, FunExpr, R> fun) {
			return fun.apply(fieldName, fieldType, object);
		}
	}

	public static class FieldTypeFunExpr extends FieldTypeFunExpr_ {
		public <R> R apply(FixieFun0<R> fun) {
			return fun.apply();
		}
	}

	public static class FieldTypeSetFunExpr extends FieldTypeFunExpr_ {
		public FunExpr value;

		public <R> R apply(FixieFun1<FunExpr, R> fun) {
			return fun.apply(value);
		}
	}

	public static class IfFunExpr extends FunExpr {
		public FunExpr then, else_;

		public <R> R apply(FixieFun2<FunExpr, FunExpr, R> fun) {
			return fun.apply(then, else_);
		}
	}

	public static class If1FunExpr extends IfFunExpr {
		public FunExpr if_;

		public <R> R apply(FixieFun1<FunExpr, R> fun) {
			return fun.apply(if_);
		}
	}

	public static class If2FunExpr extends IfFunExpr {
		public Obj_Int<Type> opcode;
		public FunExpr left, right;

		public <R> R apply(FixieFun3<Obj_Int<Type>, FunExpr, FunExpr, R> fun) {
			return fun.apply(opcode, left, right);
		}
	}

	public static class IfNonNullFunExpr extends IfFunExpr {
		public FunExpr object;

		public <R> R apply(FixieFun1<FunExpr, R> fun) {
			return fun.apply(object);
		}
	}

	public static class IndexFunExpr extends FunExpr {
		public FunExpr array;
		public FunExpr index;

		public <R> R apply(FixieFun2<FunExpr, FunExpr, R> fun) {
			return fun.apply(array, index);
		}
	}

	public static class InstanceOfFunExpr extends FunExpr {
		public FunExpr object;
		public ReferenceType instanceType;

		public <R> R apply(FixieFun2<FunExpr, ReferenceType, R> fun) {
			return fun.apply(object, instanceType);
		}
	}

	public static class InvokeMethodFunExpr extends FunExpr {
		public Class<?> clazz;
		public String methodName;
		public FunExpr object;
		public List<FunExpr> parameters;

		public <R> R apply(FixieFun4<Class<?>, String, FunExpr, List<FunExpr>, R> fun) {
			return fun.apply(clazz, methodName, object, parameters);
		}
	}

	public static class LocalFunExpr extends FunExpr {
		public int index;

		public <R> R apply(FixieFun1<Integer, R> fun) {
			return fun.apply(index);
		}
	}

	public static class NewFunExpr extends FunExpr {
		public Map<String, FunExpr> fieldValues;
		public Class<?> implementationClass, interfaceClass;

		public <R> R apply(FixieFun3<Map<String, FunExpr>, Class<?>, Class<?>, R> fun) {
			return fun.apply(fieldValues, implementationClass, interfaceClass);
		}
	}

	public static class NullFunExpr extends FunExpr {
		public <R> R apply(FixieFun0<R> fun) {
			return fun.apply();
		}
	}

	public static class PopulateFieldsFunExpr extends FunExpr {
		public FunExpr object;
		public Map<String, FunExpr> fieldValues;
		public Class<?> implementationClass, interfaceClass;

		public <R> R apply(FixieFun4<FunExpr, Map<String, FunExpr>, Class<?>, Class<?>, R> fun) {
			return fun.apply(object, fieldValues, implementationClass, interfaceClass);
		}
	}

	public static class PrintlnFunExpr extends FunExpr {
		public FunExpr expression;

		public <R> R apply(FixieFun1<FunExpr, R> fun) {
			return fun.apply(expression);
		}
	}

	public static class ProfileFunExpr extends FunExpr {
		public String counterFieldName = "p_" + Get.temp();
		public FunExpr do_;

		public <R> R apply(FixieFun2<String, FunExpr, R> fun) {
			return fun.apply(counterFieldName, do_);
		}
	}

	public static class SeqFunExpr extends FunExpr {
		public FunExpr left, right;

		public <R> R apply(FixieFun2<FunExpr, FunExpr, R> fun) {
			return fun.apply(left, right);
		}
	}

	public static class VoidFunExpr extends FunExpr {
		public <R> R apply(FixieFun0<R> fun) {
			return fun.apply();
		}
	}

}
