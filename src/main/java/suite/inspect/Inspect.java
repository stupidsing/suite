package suite.inspect;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import suite.jdk.gen.Type_;
import suite.node.util.Mutable;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.Rethrow;
import suite.util.Util;

/**
 * General manipulation on value objects with public fields.
 *
 * @author ywsing
 */
public class Inspect {

	private Map<Class<?>, List<Field>> fieldsByClass = new ConcurrentHashMap<>();

	public String toString(Object object) {
		StringBuilder sb = new StringBuilder();
		Set<Integer> ids = new HashSet<>();
		Mutable<Sink<Object>> sink = Mutable.nil();

		sink.set(o -> {
			int id = System.identityHashCode(o);
			Class<?> clazz;

			if (o == null)
				sb.append("null");
			else if ((clazz = o.getClass()).isPrimitive())
				sb.append(o.toString());
			else if (ids.add(id))
				try {
					sb.append(clazz.getSimpleName());
					sb.append("{");
					for (Field field : fields(clazz)) {
						sb.append(field.getName() + "=");
						Object value = Rethrow.reflectiveOperationException(() -> field.get(o));
						if (Type_.isSimple(field.getType()))
							sb.append(value);
						else
							sink.get().sink(value);
						sb.append(",");
					}
					sb.append("}");
				} finally {
					ids.remove(id);
				}
			else
				sb.append("<recurse>");
		});

		sink.get().sink(object);
		return sb.toString();
	}

	/**
	 * @return true if both input value objects are of the same class and having
	 *         all fields equal.
	 */
	public <T> boolean equals(T o0, T o1) {
		return o0 == o1 || o0 != null && o1 != null //
				&& o0.getClass() == o1.getClass() //
				&& Rethrow.reflectiveOperationException(() -> {
					boolean result = true;
					for (Field field : fields(o0.getClass()))
						result &= Objects.equals(field.get(o0), field.get(o1));
					return result;
				});
	}

	/**
	 * @return a combined hash code of all fields of the input value object.
	 */
	public int hashCode(Object object) {
		return Rethrow.reflectiveOperationException(() -> {
			int hashCode = 0;
			for (Field field : fields(object.getClass()))
				hashCode = hashCode * 31 + Objects.hashCode(field.get(object));
			return hashCode;
		});
	}

	/**
	 * @return the input value object recursively rewritten using the input
	 *         function.
	 */
	public <T> T rewrite(Class<T> baseClass, Fun<T, T> fun, T t0) {
		return rewrite(baseClass, null, fun, t0);
	}

	public <T> T rewrite(Class<T> baseClass, Object cp[], Fun<T, T> fun, T t0) {
		return Rethrow.reflectiveOperationException(() -> {
			T t1 = fun.apply(t0);
			T t3;
			if (t1 != null)
				t3 = t1;
			else {
				Class<?> clazz = t0.getClass();
				@SuppressWarnings("unchecked")
				T t2 = (T) Read.from(clazz.getConstructors()).uniqueResult().newInstance(cp);
				t3 = t2;
				for (Field field : fields(clazz)) {
					Object v0 = field.get(t0);
					Object v1 = rewriteValue(baseClass, cp, fun, v0);
					field.set(t3, v1);
				}
			}
			return t3;
		});
	}

	private <T> Object rewriteValue(Class<T> baseClass, Object cp[], Fun<T, T> fun, Object t0) {
		if (baseClass.isInstance(t0)) {
			@SuppressWarnings("unchecked")
			T t1 = rewrite(baseClass, cp, fun, (T) t0);
			return t1;
		} else if (Collection.class.isInstance(t0))
			return Read.from((Collection<?>) t0) //
					.map(e -> rewriteValue(baseClass, cp, fun, e)) //
					.toList();
		else
			return t0;
	}

	public List<Field> fields(Class<?> clazz) {
		List<Field> fields = fieldsByClass.get(clazz);
		if (fields == null)
			fieldsByClass.put(clazz, fields = getFields0(clazz));
		return fields;
	}

	private List<Field> getFields0(Class<?> clazz) {
		Class<?> superClass = clazz.getSuperclass();
		List<Field> parentFields = superClass != null ? fields(superClass) : Collections.emptyList();
		List<Field> childFields = Read.from(clazz.getDeclaredFields()) //
				.filter(field -> {
					int modifiers = field.getModifiers();
					return !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers);
				}).toList();

		List<Field> fields = Util.add(parentFields, childFields);
		Read.from(fields).forEach(field -> field.setAccessible(true));
		return fields;
	}

}
