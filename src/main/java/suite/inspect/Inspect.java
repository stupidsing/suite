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

	public <T> T rewrite(Class<T> baseClass, Fun<T, T> fun, T t) {
		return Rethrow.reflectiveOperationException(() -> {
			Class<?> clazz = t.getClass();
			@SuppressWarnings("unchecked")
			T expr1 = (T) clazz.newInstance();
			for (Field field : new Inspect().fields(clazz)) {
				Object value0 = field.get(t);
				@SuppressWarnings("unchecked")
				Object value1 = baseClass.isInstance(value0) ? rewrite(baseClass, fun, (T) value0) : value0;
				field.set(expr1, value1);
			}
			T expr2 = fun.apply(expr1);
			return expr2 != null ? expr2 : expr1;
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
