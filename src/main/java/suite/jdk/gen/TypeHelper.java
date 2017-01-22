package suite.jdk.gen;

import java.lang.reflect.Method;
import java.util.Objects;

import org.objectweb.asm.Type;

import suite.jdk.JdkUtil;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.Rethrow;

public class TypeHelper {

	public static int choose(Type type, int a, int d, int f, int i, int l) {
		if (Objects.equals(type, Type.DOUBLE_TYPE))
			return d;
		else if (Objects.equals(type, Type.BOOLEAN_TYPE))
			return i;
		else if (Objects.equals(type, Type.FLOAT_TYPE))
			return f;
		else if (Objects.equals(type, Type.INT_TYPE))
			return i;
		else if (Objects.equals(type, Type.LONG_TYPE))
			return l;
		else
			return a;
	}

	public static Class<?> classOf(FunExpr e) {
		return JdkUtil.getClassByName(FunType.typeOf(e).getClassName());
	}

	public static Class<?> classOf(Type type) {
		return class_(type);
	}

	public static Method methodOf(FunExpr e) {
		return methodOf(class_(FunType.typeOf(e)));
	}

	public static Method methodOf(Class<?> clazz) {
		if (clazz == Fun.class)
			return Rethrow.reflectiveOperationException(() -> Fun.class.getMethod("apply", Object.class));
		else
			return Read.from(clazz.getDeclaredMethods()).uniqueResult();
	}

	private static Class<?> class_(Type type) {
		return JdkUtil.getClassByName(type.getClassName());
	}

}
