package suite.jdk.gen;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import suite.util.FunUtil.Fun;

public class FunExpression {

	public abstract class FunExpr {
		public FunExpr apply(FunExpr... parameters) {
			ApplyFunExpr expr = new ApplyFunExpr();
			expr.object = this;
			expr.parameters = Arrays.asList(parameters);
			return expr;
		}

		public FunExpr cast(Class<?> clazz) {
			CastFunExpr expr = new CastFunExpr();
			expr.type = Type.getType(clazz);
			expr.expr = this;
			return expr;
		}

		public FunExpr checkCast(Class<?> clazz) {
			CheckCastFunExpr expr = new CheckCastFunExpr();
			expr.type = (ReferenceType) Type.getType(clazz);
			expr.expr = this;
			return expr;
		}

		public FunExpr field(String fieldName) {
			FieldFunExpr expr = new FieldFunExpr();
			expr.field = fieldName;
			expr.object = this;
			return expr;
		}

		public FunExpr field(String fieldName, Type type) {
			FieldTypeFunExpr expr = new FieldTypeFunExpr();
			expr.type = type;
			expr.field = fieldName;
			expr.object = this;
			return expr;
		}

		public FunExpr instanceOf(Class<?> clazz) {
			InstanceOfFunExpr expr = new InstanceOfFunExpr();
			expr.instanceType = (ReferenceType) Type.getType(clazz);
			expr.object = this;
			return expr;
		}

		public FunExpr invoke(String methodName, FunExpr... parameters) {
			InvokeMethodFunExpr expr = new InvokeMethodFunExpr();
			expr.methodName = methodName;
			expr.object = this;
			expr.parameters = Arrays.asList(parameters);
			return expr;
		}
	}

	public class ApplyFunExpr extends FunExpr {
		public FunExpr object;
		public List<FunExpr> parameters;
	}

	public class AssignFunExpr extends FunExpr {
		public int index;
		public FunExpr value;
	}

	public class BinaryFunExpr extends FunExpr {
		public String op;
		public FunExpr left, right;
	}

	public class CastFunExpr extends FunExpr {
		public Type type;
		public FunExpr expr;
	}

	public class CheckCastFunExpr extends FunExpr {
		public ReferenceType type;
		public FunExpr expr;
	}

	public class ConstantFunExpr extends FunExpr {
		public Type type;
		public Object constant; // primitives, class, handles etc.
	}

	public class DeclareParameterFunExpr extends FunExpr {
	}

	public class Declare1ParameterFunExpr extends DeclareParameterFunExpr {
		public Fun<FunExpr, FunExpr> doFun;
	}

	public class Declare2ParameterFunExpr extends DeclareParameterFunExpr {
		public BiFunction<FunExpr, FunExpr, FunExpr> doFun;
	}

	public class DeclareLocalFunExpr extends FunExpr {
		public FunExpr value;
		public Fun<FunExpr, FunExpr> doFun;
	}

	public class FieldFunExpr extends FunExpr {
		public FunExpr object;
		public String field;
	}

	public class FieldTypeFunExpr extends FunExpr {
		public Type type;
		public FunExpr object;
		public String field;
	}

	public class IfFunExpr extends FunExpr {
		public FunExpr then, else_;
	}

	public class If1FunExpr extends IfFunExpr {
		public FunExpr if_;
	}

	public class If2FunExpr extends IfFunExpr {
		public ToIntFunction<Type> opcode;
		public FunExpr left, right;
	}

	public class InstanceOfFunExpr extends FunExpr {
		public FunExpr object;
		public ReferenceType instanceType;
	}

	public class InvokeFunExpr extends FunExpr {
		public FunConfig<?> fun;
		public List<FunExpr> parameters;
	}

	public class InvokeMethodFunExpr extends FunExpr {
		public String methodName;
		public FunExpr object;
		public List<FunExpr> parameters;
	}

	public class LocalFunExpr extends FunExpr {
		public Type type;
		public int index;
	}

	public class ObjectFunExpr extends FunExpr {
		public Class<?> clazz;
		public Object object;
	}

	public class PrintlnFunExpr extends FunExpr {
		public FunExpr expression;
	}

	public class SeqFunExpr extends FunExpr {
		public FunExpr left, right;
	}

	public class StaticFunExpr extends FunExpr {
		public Type type;
		public String clazzType;
		public String field;
	}

}
