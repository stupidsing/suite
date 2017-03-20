package suite.jdk.gen;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import suite.inspect.Inspect;
import suite.jdk.lambda.LambdaInstance;
import suite.node.util.Singleton;
import suite.util.Util;

public class FunExpression {

	private static Inspect inspect = Singleton.get().getInspect();

	public abstract class FunExpr {
		public FunExpr apply(FunExpr... parameters) {
			return apply(Arrays.asList(parameters));
		}

		public FunExpr apply(List<FunExpr> list) {
			ApplyFunExpr expr = new ApplyFunExpr();
			expr.object = this;
			expr.parameters = list;
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
			expr.type = ObjectType.getInstance(clazz.getName());
			expr.expr = this;
			return expr;
		}

		public FunExpr field(String fieldName) {
			FieldFunExpr expr = new FieldFunExpr();
			expr.fieldName = fieldName;
			expr.object = this;
			return expr;
		}

		public FunExpr field(String fieldName, Type fieldType) {
			FieldTypeFunExpr expr = new FieldTypeFunExpr();
			expr.fieldName = fieldName;
			expr.fieldType = fieldType;
			expr.object = this;
			return expr;
		}

		public FunExpr index(FunExpr index) {
			IndexFunExpr expr = new IndexFunExpr();
			expr.array = this;
			expr.index = index;
			return expr;
		}

		public FunExpr instanceOf(Class<?> clazz) {
			InstanceOfFunExpr expr = new InstanceOfFunExpr();
			expr.instanceType = (ReferenceType) Type.getType(clazz);
			expr.object = this;
			return expr;
		}

		public FunExpr invoke(String methodName, FunExpr... parameters) {
			return invoke(null, methodName, parameters);
		}

		public FunExpr invoke(Class<?> clazz, String methodName, FunExpr... parameters) {
			return invoke(clazz, methodName, Arrays.asList(parameters));
		}

		public FunExpr invoke(Class<?> clazz, String methodName, List<FunExpr> list) {
			InvokeMethodFunExpr expr = new InvokeMethodFunExpr();
			expr.clazz = clazz;
			expr.methodName = methodName;
			expr.object = this;
			expr.parameters = list;
			return expr;
		}

		@Override
		public boolean equals(Object object) {
			return Util.clazz(object) == getClass() && inspect.equals(this, object);
		}

		@Override
		public int hashCode() {
			return inspect.hashCode(this);
		}

		@Override
		public String toString() {
			return inspect.toString(this);
		}
	}

	public class ApplyFunExpr extends FunExpr {
		public FunExpr object;
		public List<FunExpr> parameters;
	}

	public class AssignLocalFunExpr extends FunExpr {
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
		public FunExpr do_;
	}

	public class Declare0ParameterFunExpr extends DeclareParameterFunExpr {
	}

	public class Declare1ParameterFunExpr extends DeclareParameterFunExpr {
		public PlaceholderFunExpr parameter;
	}

	public class Declare2ParameterFunExpr extends DeclareParameterFunExpr {
		public PlaceholderFunExpr p0, p1;
	}

	public class DeclareLocalFunExpr extends FunExpr {
		public PlaceholderFunExpr var;
		public FunExpr value;
		public FunExpr do_;
	}

	public class FieldFunExpr extends FunExpr {
		public String fieldName;
		public FunExpr object;
	}

	public class FieldInjectFunExpr extends FunExpr {
		public String fieldName;
	}

	public class FieldStaticFunExpr extends FunExpr {
		public String fieldName;
		public Type fieldType;
	}

	public class FieldTypeFunExpr extends FunExpr {
		public String fieldName;
		public Type fieldType;
		public FunExpr object;
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

	public class IfNonNullFunExpr extends IfFunExpr {
		public FunExpr object;
	}

	public class IndexFunExpr extends FunExpr {
		public FunExpr array;
		public FunExpr index;
	}

	public class InstanceOfFunExpr extends FunExpr {
		public FunExpr object;
		public ReferenceType instanceType;
	}

	public class InvokeLambdaFunExpr extends FunExpr {
		public boolean isExpand;
		public LambdaInstance<?> lambda;
		public List<FunExpr> parameters;
	}

	public class InvokeMethodFunExpr extends FunExpr {
		public Class<?> clazz;
		public String methodName;
		public FunExpr object;
		public List<FunExpr> parameters;
	}

	public class LocalFunExpr extends FunExpr {
		public int index;
	}

	public class NewFunExpr extends FunExpr {
		public String className;
		public Map<String, FunExpr> fieldValues;
		public Class<?> implementationClass, interfaceClass;
	}

	public class ObjectFunExpr extends FunExpr {
		public Type type;
		public Object object;
	}

	public class PlaceholderFunExpr extends FunExpr {
		public int id = Util.temp();
	}

	public class PrintlnFunExpr extends FunExpr {
		public FunExpr expression;
	}

	public class ProfileFunExpr extends FunExpr {
		public String counterFieldName = "p" + Util.temp();
		public FunExpr do_;
	}

	public class SeqFunExpr extends FunExpr {
		public FunExpr left, right;
	}

}
