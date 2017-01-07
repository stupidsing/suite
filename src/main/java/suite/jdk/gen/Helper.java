package suite.jdk.gen;

import java.lang.reflect.Method;

import org.objectweb.asm.Type;

import suite.jdk.JdkUtil;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.Rethrow;

public class Helper {

	public static Helper instance = new Helper();

	private Helper() {
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
