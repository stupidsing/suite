package suite.inspect;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

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

		BiConsumer<Class<?>, Object> append = (clazz, object_) -> {
			if (object_ == null)
				sb.append("null");
			else if (Type_.isSimple(clazz))
				sb.append(object_);
			else {
				int id = System.identityHashCode(object_);
				if (ids.add(id))
					try {
						sink.get().sink(object_);
					} finally {
						ids.remove(id);
					}
				else
					sb.append("<recurse>");
			}
		};

		Sink<Object> app = object_ -> append.accept(Util.clazz(object_), object_);

		sink.set(object_ -> {
			Extract_ inspect_ = new Extract_(object_);
			String prefix = inspect_.prefix;
			Class<?> keyClass = inspect_.keyClass;
			ExtractField iter = inspect_.children;

			if (Util.stringEquals(prefix, "[")) {
				sb.append("[");
				while (iter.next()) {
					append.accept(keyClass, iter.getKey());
					sb.append(",");
				}
				sb.append("]");
			} else {
				sb.append(prefix);
				if (!Util.stringEquals(prefix, "{"))
					sb.append("{");
				while (iter.next()) {
					append.accept(keyClass, iter.getKey());
					sb.append("=");
					append.accept(iter.getValueClass(), iter.getValue());
					sb.append(",");
				}
				sb.append("}");
			}
		});

		app.sink(object);
		return sb.toString();
	}

	private class Extract_ {
		private String prefix;
		private Class<?> keyClass;
		private ExtractField children;

		private Extract_(Object object) {
			Class<?> clazz = object.getClass();

			if (clazz.isArray()) {
				int length = Array.getLength(object);

				prefix = "[";
				keyClass = clazz.getComponentType();
				children = new InspectCollection() {
					private int i = -1;

					public boolean next() {
						return ++i < length;
					}

					public Object getKey() {
						return Array.get(object, i);
					}
				};
			} else if (Collection.class.isAssignableFrom(clazz)) {
				Class<?> elementClass_ = Object.class;
				ParameterizedType pt;

				for (Type genericInterface : clazz.getGenericInterfaces())
					if (genericInterface instanceof ParameterizedType //
							&& (pt = (ParameterizedType) genericInterface).getRawType() == Collection.class)
						elementClass_ = (Class<?>) pt.getActualTypeArguments()[0];

				@SuppressWarnings("unchecked")
				Iterator<Object> iter = ((Collection<Object>) object).iterator();

				prefix = "[";
				keyClass = elementClass_;
				children = new InspectCollection() {
					private Object element;

					public boolean next() {
						if (iter.hasNext()) {
							element = iter.next();
							return true;
						} else
							return false;
					}

					public Object getKey() {
						return element;
					}
				};
			} else if (Map.class.isAssignableFrom(clazz))

			{
				Class<?> valueClass_ = Object.class;
				ParameterizedType pt;
				Type typeArgs[];

				for (Type genericInterface : clazz.getGenericInterfaces())
					if (genericInterface instanceof ParameterizedType //
							&& (pt = (ParameterizedType) genericInterface).getRawType() == Map.class //
							&& 1 < (typeArgs = pt.getActualTypeArguments()).length //
							&& typeArgs[0] instanceof Class //
							&& typeArgs[1] instanceof Class) {
						typeArgs = pt.getActualTypeArguments();
						keyClass = (Class<?>) typeArgs[0];
						valueClass_ = (Class<?>) typeArgs[1];
					}

				@SuppressWarnings("unchecked")
				Iterator<Entry<Object, Object>> iter = ((Map<Object, Object>) object).entrySet().iterator();
				Class<?> valueClass = valueClass_;

				prefix = "{";
				keyClass = Object.class;
				children = new ExtractField() {
					private Entry<Object, Object> entry;

					public boolean next() {
						if (iter.hasNext()) {
							entry = iter.next();
							return true;
						} else
							return false;
					}

					public Object getKey() {
						return entry.getKey();
					}

					public Class<?> getValueClass() {
						return valueClass;
					}

					public Object getValue() {
						return entry.getValue();
					}
				};
			} else {
				Iterator<Field> iter = fields(clazz).iterator();

				prefix = clazz.getSimpleName();
				keyClass = String.class;
				children = new ExtractField() {
					private Field field;

					public boolean next() {
						if (iter.hasNext()) {
							field = iter.next();
							return true;
						} else
							return false;
					}

					public Object getKey() {
						return field.getName();
					}

					public Class<?> getValueClass() {
						return field.getType();
					}

					public Object getValue() {
						return Rethrow.reflectiveOperationException(() -> field.get(object));
					}
				};
			}
		}
	}

	private abstract class InspectCollection implements ExtractField {
		public Class<?> getValueClass() {
			return null;
		}

		public Object getValue() {
			return null;
		}
	}

	private interface ExtractField {
		public boolean next();

		public Object getKey();

		public Class<?> getValueClass();

		public Object getValue();
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
