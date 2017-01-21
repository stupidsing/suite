package suite.inspect;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.Rethrow;
import suite.util.Util;

/**
 * Retrieve list of fields of a value object, and provide shallow
 * equals()/hashCode() methods.
 *
 * @author ywsing
 */
public class Inspect {

	private Map<Class<?>, List<Field>> fieldsByClass = new ConcurrentHashMap<>();

	public <T> boolean equals(T o0, T o1) {
		return o0 == o1 //
				|| o0 != null && o1 != null && o0.getClass() == o1.getClass() && Objects.equals(list(o0), list(o1));
	}

	public int hashCode(Object object) {
		return list(object).hashCode();
	}

	private List<Object> list(Object object) {
		return Read.from(fields(object.getClass())) //
				.map(field -> {
					try {
						return field.get(object);
					} catch (IllegalAccessException ex) {
						throw new RuntimeException(ex);
					}
				}) //
				.toList();
	}

	public <T> T rewrite(Class<T> baseClass, Fun<T, T> fun, T t0) {
		return rewrite(baseClass, null, fun, t0);
	}

	public <T> T rewrite(Class<T> baseClass, Object ctorParameters[], Fun<T, T> fun, T t0) {
		return Rethrow.reflectiveOperationException(() -> {
			T t1 = fun.apply(t0);
			T t3;
			if (t1 != null)
				t3 = t1;
			else {
				Class<?> clazz = t0.getClass();
				@SuppressWarnings("unchecked")
				T t2 = (T) Read.from(clazz.getConstructors()).uniqueResult().newInstance(ctorParameters);
				t3 = t2;
				for (Field field : fields(clazz)) {
					Object v0 = field.get(t0);
					@SuppressWarnings("unchecked")
					Object v1 = baseClass.isInstance(v0) ? rewrite(baseClass, ctorParameters, fun, (T) v0) : v0;
					field.set(t3, v1);
				}
			}
			return t3;
		});
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
