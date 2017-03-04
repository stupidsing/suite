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
import suite.util.Util;

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
		return clazz.isPrimitive() //
				|| clazz == Class.class //
				|| clazz == Date.class //
				|| clazz == String.class //
				|| clazz == Timestamp.class;
	}

	public static Method methodOf(Class<?> clazz) {
		if (clazz == BiFunction.class)
			return Rethrow.reflectiveOperationException(() -> clazz.getMethod("apply", Object.class, Object.class));
		else if (clazz == BiPredicate.class)
			return Rethrow.reflectiveOperationException(() -> clazz.getMethod("test", Object.class, Object.class));
		else if (clazz == Fun.class || clazz == Function.class)
			return Rethrow.reflectiveOperationException(() -> clazz.getMethod("apply", Object.class));
		else if (clazz == Predicate.class)
			return Rethrow.reflectiveOperationException(() -> clazz.getMethod("test", Object.class));
		else if (clazz == Sink.class)
			return Rethrow.reflectiveOperationException(() -> clazz.getMethod("sink", Object.class));
		else if (clazz == Source.class)
			return Rethrow.reflectiveOperationException(() -> clazz.getMethod("source"));
		else
			return Read.from(clazz.getDeclaredMethods()).uniqueResult();
	}

	private static Class<?> getClassByName(String className) {
		return Rethrow.reflectiveOperationException(() -> {
			if (Util.stringEquals(className, "byte"))
				return byte.class;
			else if (Util.stringEquals(className, "char"))
				return char.class;
			else if (Util.stringEquals(className, "boolean"))
				return boolean.class;
			else if (Util.stringEquals(className, "double"))
				return double.class;
			else if (Util.stringEquals(className, "float"))
				return float.class;
			else if (Util.stringEquals(className, "int"))
				return int.class;
			else if (Util.stringEquals(className, "long"))
				return long.class;
			else if (Util.stringEquals(className, "short"))
				return short.class;
			else if (Util.stringEquals(className, "void"))
				return void.class;
			else
				return Class.forName(className);
		});
	}

}
