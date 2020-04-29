package suite.util;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.Date;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import javassist.Modifier;
import primal.MoreVerbs.Read;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Sink;
import primal.fp.Funs.Source;
import primal.fp.Funs2.Fun2;

public class Util {

	public static void assert_(boolean b) {
		if (!b)
			throw new AssertionError();
	}

	public static boolean isSimple(Class<?> clazz) {
		return clazz.isEnum()
				|| clazz.isPrimitive()
				|| clazz == Boolean.class
				|| clazz == Class.class
				|| clazz == Date.class
				|| clazz == String.class
				|| clazz == Timestamp.class
				|| Number.class.isAssignableFrom(clazz);
	}

	public static Method methodOf(Class<?> clazz) {
		if (clazz == BiPredicate.class)
			return ex(() -> clazz.getMethod("test", Object.class, Object.class));
		else if (clazz == Fun.class || clazz == Function.class)
			return ex(() -> clazz.getMethod("apply", Object.class));
		else if (clazz == Fun2.class)
			return ex(() -> clazz.getMethod("apply", Object.class, Object.class));
		else if (clazz == Predicate.class)
			return ex(() -> clazz.getMethod("test", Object.class));
		else if (clazz == Sink.class)
			return ex(() -> clazz.getMethod("sink", Object.class));
		else if (clazz == Source.class)
			return ex(() -> clazz.getMethod("g"));
		else
			try {
				return Read
						.from(clazz.getDeclaredMethods())
						.filter(m -> !m.isDefault() && !m.isSynthetic() && !Modifier.isStatic(m.getModifiers()))
						.uniqueResult();
			} catch (Exception ex) {
				return fail("for " + clazz, ex);
			}
	}

	public static <T extends Exception, R> R throwSneakly(Exception ex) throws T {
		@SuppressWarnings("unchecked")
		var t = (T) ex;
		throw t;
	}

}
