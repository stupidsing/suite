package suite.jdk.gen;

import java.lang.reflect.Method;

import org.apache.bcel.Const;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import suite.jdk.JdkUtil;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.Rethrow;

public class Type_ {

	public static Method methodOf(Class<?> clazz) {
		if (clazz == Fun.class)
			return Rethrow.reflectiveOperationException(() -> Fun.class.getMethod("apply", Object.class));
		else
			return Read.from(clazz.getDeclaredMethods()).uniqueResult();
	}

	public static Class<?> classOf(Type type) {
		if (type instanceof ObjectType)
			return JdkUtil.getClassByName(((ObjectType) type).getClassName());
		else if (type instanceof BasicType)
			return JdkUtil.getClassByName(Const.getTypeName(((BasicType) type).getType()));
		else
			throw new RuntimeException();
	}

}
