package suite.jdk.gen;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

import org.objectweb.asm.Type;

import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.Rethrow;

public class FunExpression {

	public final FunConstructor fc;

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
			expr.type = Type.getType(clazz);
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
			expr.instanceType = clazz;
			expr.object = this;
			return expr;
		}

		public FunExpr invoke(String methodName, FunExpr... parameters) {
			InvokeFunExpr expr = new InvokeFunExpr();
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
		public ToIntFunction<Type> opcode;
		public FunExpr left, right;
	}

	public class CastFunExpr extends FunExpr {
		public Type type;
		public FunExpr expr;
	}

	public class CheckCastFunExpr extends FunExpr {
		public Type type;
		public FunExpr expr;
	}

	public class ConstantFunExpr extends FunExpr {
		public Type type;
		public Object constant; // primitives, class, handles etc.
	}

	public class DeclareParameterFunExpr extends FunExpr {
		public Class<?> interfaceClass;
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
		public Class<?> instanceType;
	}

	public class InvokeFunExpr extends FunExpr {
		public String methodName;
		public FunExpr object;
		public List<FunExpr> parameters;

		public Method method() {
			Type array[] = Read.from(parameters) //
					.map(FunType::typeOf) //
					.toList() //
					.toArray(new Type[0]);

			@SuppressWarnings("unchecked")
			List<Class<?>> parameterTypes = (List<Class<?>>) (List<?>) Read.from(array) //
					.map(TypeHelper.instance::classOf) //
					.toList();

			return Rethrow.reflectiveOperationException(() -> {
				return TypeHelper.instance.classOf(object).getMethod(methodName, parameterTypes.toArray(new Class<?>[0]));
			});
		}
	}

	public class LocalFunExpr extends FunExpr {
		public Type type;
		public int index;
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

	public FunExpression(FunConstructor fc) {
		this.fc = fc;
	}

}
