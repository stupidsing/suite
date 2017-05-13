package suite.jdk.gen;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.Date;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.bcel.Const;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.Rethrow;
import suite.util.String_;

public class Type_ {

	public static Class<?> classOf(Type type) {
		if (type instanceof ObjectType)
			return getClassByName(((ObjectType) type).getClassName());
		else if (type instanceof BasicType)
			return getClassByName(Const.getTypeName(((BasicType) type).getType()));
		else
			throw new RuntimeException();
	}

	public static boolean isSimple(Class<?> clazz) {
		return clazz.isEnum() //
				|| clazz.isPrimitive() //
				|| clazz == Boolean.class //
				|| clazz == Class.class //
				|| clazz == Date.class //
				|| clazz == String.class //
				|| clazz == Timestamp.class //
				|| Number.class.isAssignableFrom(clazz);
	}

	public static Method methodOf(Class<?> clazz) {
		if (clazz == BiFunction.class)
			return Rethrow.ex(() -> clazz.getMethod("apply", Object.class, Object.class));
		else if (clazz == BiPredicate.class)
			return Rethrow.ex(() -> clazz.getMethod("test", Object.class, Object.class));
		else if (clazz == Fun.class || clazz == Function.class)
			return Rethrow.ex(() -> clazz.getMethod("apply", Object.class));
		else if (clazz == Predicate.class)
			return Rethrow.ex(() -> clazz.getMethod("test", Object.class));
		else if (clazz == Sink.class)
			return Rethrow.ex(() -> clazz.getMethod("sink", Object.class));
		else if (clazz == Source.class)
			return Rethrow.ex(() -> clazz.getMethod("source"));
		else
			return Read.from(clazz.getDeclaredMethods()).uniqueResult();
	}

	private static Class<?> getClassByName(String className) {
		return Rethrow.ex(() -> {
			if (String_.equals(className, "byte"))
				return byte.class;
			else if (String_.equals(className, "char"))
				return char.class;
			else if (String_.equals(className, "boolean"))
				return boolean.class;
			else if (String_.equals(className, "double"))
				return double.class;
			else if (String_.equals(className, "float"))
				return float.class;
			else if (String_.equals(className, "int"))
				return int.class;
			else if (String_.equals(className, "long"))
				return long.class;
			else if (String_.equals(className, "short"))
				return short.class;
			else if (String_.equals(className, "void"))
				return void.class;
			else
				return Class.forName(className);
		});
	}

}
