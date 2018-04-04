package suite.inspect;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import suite.adt.pair.Pair;
import suite.jdk.gen.Type_;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil2.Source2;
import suite.util.List_;
import suite.util.Memoize;
import suite.util.Object_;
import suite.util.Rethrow;
import suite.util.String_;

/**
 * General manipulation on value objects with public fields.
 *
 * @author ywsing
 */
public class Inspect {

	public interface Property {
		public Object get(Object object);

		public void set(Object object, Object value);
	}

	/**
	 * @return true if both input value objects are of the same class and having all
	 *         fields equal.
	 */
	public <T> boolean equals(T o0, T o1) {
		return o0 == o1 || o0 != null && o1 != null //
				&& o0.getClass() == o1.getClass() //
				&& Rethrow.ex(() -> {
					boolean b = true;
					for (Field field : fields(o0.getClass()))
						b &= Objects.equals(field.get(o0), field.get(o1));
					return b;
				});
	}

	/**
	 * @return a combined hash code of all fields of the input value object.
	 */
	public int hashCode(Object object) {
		return Rethrow.ex(() -> {
			var h = 7;
			for (Field field : fields(object.getClass()))
				h = h * 31 + Objects.hashCode(field.get(object));
			return h;
		});
	}

	public String toString(Object object) {
		StringBuilder sb = new StringBuilder();
		Set<Integer> ids = new HashSet<>();

		new Object() {
			private void append(Class<?> clazz, Object object_) {
				int id;

				if (object_ == null)
					sb.append("null");
				else if (Type_.isSimple(clazz))
					sb.append(object_);
				else if (ids.add(id = System.identityHashCode(object_)))
					try {
						Extract extract_ = new Extract(object_);
						String prefix = extract_.prefix;
						Class<?> keyClass = extract_.keyClass;
						ExtractField iter = extract_.children;

						if (String_.equals(prefix, "[")) {
							sb.append("[");
							while (iter.next()) {
								append(keyClass, iter.getKey());
								sb.append(",");
							}
							sb.append("]");
						} else {
							sb.append(prefix);
							if (!String_.equals(prefix, "{"))
								sb.append("{");
							while (iter.next()) {
								append(keyClass, iter.getKey());
								sb.append("=");
								append(iter.getValueClass(), iter.getValue());
								sb.append(",");
							}
							sb.append("}");
						}
					} finally {
						ids.remove(id);
					}
				else
					sb.append("<recurse>");
			}
		}.append(Object_.clazz(object), object);

		return sb.toString();
	}

	public List<Field> fields(Class<?> clazz) {
		return fieldsFun.apply(clazz);
	}

	public List<Method> getters(Class<?> clazz) {
		return gettersFun.apply(clazz);
	}

	public List<Method> methods(Class<?> clazz) {
		return methodsFun.apply(clazz);
	}

	public List<Property> properties(Class<?> clazz) {
		return propertiesFun.apply(clazz);
	}

	private Fun<Class<?>, List<Field>> fieldsFun = Memoize.funRec(clazz -> {
		Class<?> superClass = clazz.getSuperclass();

		// do not display same field of different base classes
		Set<String> names = new HashSet<>();
		List<Field> parentFields = superClass != null ? fields(superClass) : List.of();
		List<Field> childFields = Read //
				.from(clazz.getDeclaredFields()) //
				.filter(field -> {
					var modifiers = field.getModifiers();
					String name = field.getName();
					return !Modifier.isStatic(modifiers) //
							&& !Modifier.isTransient(modifiers) //
							&& !name.startsWith("this") //
							&& names.add(name);
				}) //
				.toList();

		List<Field> fields = List_.concat(parentFields, childFields);
		fields.forEach(field -> field.setAccessible(true));
		return fields;
	});

	private Fun<Class<?>, List<Method>> gettersFun = Memoize.funRec(clazz -> {
		return Read //
				.from(methods(clazz)) //
				.filter(getter -> {
					String name = getter.getName();
					return name.startsWith("get") && getter.getParameterTypes().length == 0;
				}) //
				.toList();
	});

	private Fun<Class<?>, List<Method>> methodsFun = Memoize.funRec(clazz -> {
		Class<?> superClass = clazz.getSuperclass();

		// do not display same method of different base classes
		Set<String> names = new HashSet<>();
		List<Method> parentMethods = superClass != null ? methods(superClass) : List.of();
		List<Method> childMethods = Read //
				.from(clazz.getDeclaredMethods()) //
				.filter(method -> {
					var modifiers = method.getModifiers();
					return !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers) && names.add(method.getName());
				}) //
				.toList();

		List<Method> methods = List_.concat(parentMethods, childMethods);
		Read.from(methods).filter(method -> method.getDeclaringClass() != Object.class).sink(method -> method.setAccessible(true));
		return methods;
	});

	private Fun<Class<?>, List<Property>> propertiesFun = Memoize.funRec(clazz -> {
		List<Method> methods = methods(clazz);

		Map<String, Method> getMethods = Read //
				.from(methods) //
				.filter(getter -> {
					String name = getter.getName();
					return name.startsWith("get") && getter.getParameterTypes().length == 0;
				}) //
				.map2(getter -> getter.getName().substring(3), getter -> getter) //
				.toMap();

		Map<String, Method> setMethods = Read //
				.from(methods) //
				.filter(setter -> {
					String name = setter.getName();
					return name.startsWith("set") && setter.getParameterTypes().length == 1;
				}) //
				.map2(setter -> setter.getName().substring(3), setter -> setter) //
				.toMap();

		Set<String> propertyNames = new HashSet<>(getMethods.keySet());
		propertyNames.retainAll(setMethods.keySet());

		return Read //
				.from(propertyNames) //
				.<Property> map(propertyName -> {
					Method getMethod = getMethods.get(propertyName);
					Method setMethod = setMethods.get(propertyName);
					return new Property() {
						public Object get(Object object) {
							return Rethrow.ex(() -> getMethod.invoke(object));
						}

						public void set(Object object, Object value) {
							Rethrow.ex(() -> setMethod.invoke(object, value));
						}
					};
				}) //
				.toList();
	});

	private class Extract {
		private String prefix;
		private Class<?> keyClass;
		private ExtractField children;

		private Extract(Object object) {
			this(object, false);
		}

		private Extract(Object object, boolean isDumpGetters) {
			Class<?> clazz = object.getClass();

			if (clazz.isArray()) {
				var length = Array.getLength(object);

				prefix = "[";
				keyClass = clazz.getComponentType();
				children = new ExtractCollection() {
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
				Type[] typeArgs;
				Type typeArg;

				for (Type genericInterface : clazz.getGenericInterfaces())
					if (genericInterface instanceof ParameterizedType //
							&& (pt = (ParameterizedType) genericInterface).getRawType() == Collection.class //
							&& 1 < (typeArgs = pt.getActualTypeArguments()).length //
							&& (typeArg = typeArgs[0]) instanceof Class)
						elementClass_ = (Class<?>) typeArg;

				@SuppressWarnings("unchecked")
				Iterator<Object> iter = ((Collection<Object>) object).iterator();

				prefix = "[";
				keyClass = elementClass_;
				children = new ExtractCollection() {
					private Object element;

					public boolean next() {
						boolean b = iter.hasNext();
						if (b)
							element = iter.next();
						return b;
					}

					public Object getKey() {
						return element;
					}
				};
			} else if (Map.class.isAssignableFrom(clazz)) {
				Class<?> valueClass_ = Object.class;
				ParameterizedType pt;
				Type[] typeArgs;
				Type typeArg0, typeArg1;

				for (Type genericInterface : clazz.getGenericInterfaces())
					if (genericInterface instanceof ParameterizedType //
							&& (pt = (ParameterizedType) genericInterface).getRawType() == Map.class //
							&& 1 < (typeArgs = pt.getActualTypeArguments()).length //
							&& (typeArg0 = typeArgs[0]) instanceof Class //
							&& (typeArg1 = typeArgs[1]) instanceof Class) {
						typeArgs = pt.getActualTypeArguments();
						keyClass = (Class<?>) typeArg0;
						valueClass_ = (Class<?>) typeArg1;
					}

				@SuppressWarnings("unchecked")
				Iterator<Entry<Object, Object>> iter = ((Map<Object, Object>) object).entrySet().iterator();
				Class<?> valueClass = valueClass_;

				prefix = "{";
				keyClass = Object.class;
				children = new ExtractField() {
					private Entry<Object, Object> entry;

					public boolean next() {
						boolean b = iter.hasNext();
						if (b)
							entry = iter.next();
						return b;
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
				prefix = clazz.getSimpleName();
				keyClass = String.class;

				if (isDumpGetters) {
					Source2<String, Method> source = Read //
							.from(clazz.getMethods()) //
							.filter(method -> method.getParameterTypes().length == 0) //
							.groupBy(Method::getName, Streamlet::first) //
							.filterKey(name -> name.startsWith("get")) //
							.source();

					children = new ExtractField() {
						private Pair<String, Method> pair = Pair.of(null, null);

						public boolean next() {
							return source.source2(pair);
						}

						public Object getKey() {
							return pair.t0;
						}

						public Class<?> getValueClass() {
							return pair.t1.getReturnType();
						}

						public Object getValue() {
							return Rethrow.ex(() -> pair.t1.invoke(object));
						}
					};
				} else {
					Iterator<Field> iter = fields(clazz).iterator();

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
							return Rethrow.ex(() -> field.get(object));
						}
					};
				}
			}
		}
	}

	private abstract class ExtractCollection implements ExtractField {
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
	 * @return the input value object recursively rewritten using the input
	 *         function.
	 */
	public <T> T rewrite(Class<T> baseClass, Iterate<T> fun, T t0) {
		return new Object() {
			private T rewrite(T t0) {
				return Rethrow.ex(() -> {
					T t1 = fun.apply(t0);
					return t1 != null ? t1 : mapFields(t0, this::rewriteField);
				});
			}

			private Object rewriteField(Object t0) {
				if (baseClass.isInstance(t0)) {
					@SuppressWarnings("unchecked")
					T t1 = rewrite((T) t0);
					return t1;
				} else if (t0 instanceof Collection)
					return Read.from((Collection<?>) t0).map(this::rewriteField).toList();
				else if (t0 instanceof Pair) {
					Pair<?, ?> t1 = (Pair<?, ?>) t0;
					return Pair.of(rewriteField(t1.t0), rewriteField(t1.t1));
				} else
					return t0;
			}
		}.rewrite(t0);
	}

	private <T> T mapFields(T t0, Fun<Object, Object> mapper) throws ReflectiveOperationException {
		Class<?> clazz = t0.getClass();
		@SuppressWarnings("unchecked")
		T t1 = (T) Read.from(clazz.getConstructors()).uniqueResult().newInstance();
		for (Field field : fields(clazz)) {
			Object v0 = field.get(t0);
			Object v1 = mapper.apply(v0);
			field.set(t1, v1);
		}
		return t1;
	}

}
