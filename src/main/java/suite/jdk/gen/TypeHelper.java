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

	public static TypeHelper instance = new TypeHelper();

	private TypeHelper() {
	}

	public int choose(Type type, int a, int d, int f, int i, int l) {
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

	public Class<?> clazz(FunExpr e) {
		return JdkUtil.getClassByName(FunType.typeOf(e).getClassName());
	}

	public Class<?> clazz(Type type) {
		return class_(type);
	}

	public Method method(FunExpr e) {
		return method(class_(FunType.typeOf(e)));
	}

	public Method method(Class<?> clazz) {
		if (clazz == Fun.class)
			return Rethrow.reflectiveOperationException(() -> Fun.class.getMethod("apply", Object.class));
		else
			return Read.from(clazz.getDeclaredMethods()).uniqueResult();
	}

	private Class<?> class_(Type type) {
		return JdkUtil.getClassByName(type.getClassName());
	}

}
