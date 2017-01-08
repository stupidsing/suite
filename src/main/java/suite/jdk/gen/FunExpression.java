package suite.jdk.gen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import org.objectweb.asm.Type;

import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.Rethrow;

public class FunExpression {

	public final FunCreator<?> fc;

	public abstract class FunExpr {
		public FunExpr apply(FunExpr... parameters) {
			ApplyFunExpr expr = new ApplyFunExpr();
			expr.object = this;
			expr.parameters = parameters;
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
			return Rethrow.reflectiveOperationException(() -> {
				Field field = Helper.instance.clazz(this).getField(fieldName);
				return cast(field.getDeclaringClass()).field(fieldName, Type.getType(field.getType()));
			});
		}

		public FunExpr field(String fieldName, Type type) {
			FieldFunExpr expr = new FieldFunExpr();
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
		public FunExpr parameters[];
	}

	public class AssignFunExpr extends FunExpr {
		public int index;
		public FunExpr value;
	}

	public class BinaryFunExpr extends FunExpr {
		public Fun<Type, Integer> opcode;
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
		public Fun<Type, Integer> opcode;
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
					.map(Helper.instance::clazz) //
					.toList();

			return Rethrow.reflectiveOperationException(() -> {
				return Helper.instance.clazz(object).getMethod(methodName, parameterTypes.toArray(new Class<?>[0]));
			});
		}
	}

	public class LocalFunExpr extends FunExpr {
		public Type type;
		public int index;
	}

	public class NoOperationFunExpr extends FunExpr {
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

	public FunExpression(FunCreator<?> fc) {
		this.fc = fc;
	}

}
