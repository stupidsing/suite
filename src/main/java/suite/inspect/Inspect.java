package suite.inspect;

import static primal.statics.Rethrow.ex;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import primal.MoreVerbs.Read;
import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Iterate;
import primal.streamlet.Streamlet;
import suite.object.ObjectSupport;
import suite.util.Memoize;

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
		return new ObjectSupport<>(this::values).equals(o0, o1);
	}

	/**
	 * @return a combined hash code of all fields of the input value object.
	 */
	public int hashCode(Object object) {
		return new ObjectSupport<>(this::values).hashCode(object);
	}

	public List<?> values(Object object) {
		return fields(object.getClass()) //
				.map(field -> ex(() -> field.get(object))) //
				.toList();
	}

	public Streamlet<Field> fields(Class<?> clazz) {
		return fieldsFun.apply(clazz);
	}

	public Streamlet<Method> getters(Class<?> clazz) {
		return gettersFun.apply(clazz);
	}

	public Streamlet<Method> methods(Class<?> clazz) {
		return methodsFun.apply(clazz);
	}

	public Streamlet<Property> properties(Class<?> clazz) {
		return propertiesFun.apply(clazz);
	}

	private Fun<Class<?>, Streamlet<Field>> fieldsFun = Memoize.funRec(clazz -> {
		var superClass = clazz.getSuperclass();

		// do not display same field of different base classes
		var names = new HashSet<>();
		var parentFields = superClass != null ? fields(superClass) : Read.<Field> empty();
		var childFields = Read //
				.from(clazz.getDeclaredFields()) //
				.filter(field -> {
					var modifiers = field.getModifiers();
					var name = field.getName();
					return !Modifier.isStatic(modifiers) //
							&& !Modifier.isTransient(modifiers) //
							&& !name.startsWith("this") //
							&& names.add(name);
				}) //
				.collect();

		return Streamlet //
				.concat(parentFields, childFields) //
				.filter(field -> field.trySetAccessible());
	});

	private Fun<Class<?>, Streamlet<Method>> gettersFun = Memoize.funRec(clazz -> {
		return methods(clazz) //
				.filter(getter -> {
					return getter.getName().startsWith("get") && getter.getParameterTypes().length == 0;
				}) //
				.collect();
	});

	private Fun<Class<?>, Streamlet<Method>> methodsFun = Memoize.funRec(clazz -> {
		var superClass = clazz.getSuperclass();

		// do not display same method of different base classes
		var names = new HashSet<>();
		var parentMethods = superClass != null ? methods(superClass) : Read.<Method> empty();
		var childMethods = Read //
				.from(clazz.getDeclaredMethods()) //
				.filter(m -> {
					var mods = m.getModifiers();
					return !Modifier.isStatic(mods) && !Modifier.isTransient(mods) && names.add(m.getName());
				}) //
				.collect();

		return Streamlet //
				.concat(parentMethods, childMethods) //
				.filter(method -> method.getDeclaringClass() != Object.class && method.trySetAccessible());
	});

	private Fun<Class<?>, Streamlet<Property>> propertiesFun = Memoize.funRec(clazz -> {
		var methods = methods(clazz);

		var getMethods = methods //
				.filter(getter -> {
					return getter.getName().startsWith("get") && getter.getParameterTypes().length == 0;
				}) //
				.map2(getter -> getter.getName().substring(3), getter -> getter) //
				.toMap();

		var setMethods = methods //
				.filter(setter -> {
					return setter.getName().startsWith("set") && setter.getParameterTypes().length == 1;
				}) //
				.map2(setter -> setter.getName().substring(3), setter -> setter) //
				.toMap();

		var propertyNames = new HashSet<>(getMethods.keySet());
		propertyNames.retainAll(setMethods.keySet());

		return Read //
				.from(propertyNames) //
				.<Property> map(propertyName -> {
					var getMethod = getMethods.get(propertyName);
					var setMethod = setMethods.get(propertyName);
					return new Property() {
						public Object get(Object object) {
							return ex(() -> getMethod.invoke(object));
						}

						public void set(Object object, Object value) {
							ex(() -> setMethod.invoke(object, value));
						}
					};
				}) //
				.collect();
	});

	/**
	 * @return the input value object recursively rewritten using the input
	 *         function.
	 */
	public <T> T rewrite(T t0, Class<T> baseClass, Iterate<T> fun) {
		return new Object() {
			private T rewrite(T t0) {
				return ex(() -> {
					var t1 = fun.apply(t0);
					return t1 != null ? t1 : mapFields(t0, this::rewriteField);
				});
			}

			private Object rewriteField(Object t) {
				if (baseClass.isInstance(t)) {
					@SuppressWarnings("unchecked")
					var t1 = rewrite((T) t);
					return t1;
				} else if (t instanceof Collection<?> c)
					return Read.from(c).map(this::rewriteField).toList();
				else if (t instanceof Pair<?, ?> p) {
					return Pair.of(rewriteField(p.k), rewriteField(p.v));
				} else
					return t;
			}
		}.rewrite(t0);
	}

	private <T> T mapFields(T t0, Fun<Object, Object> mapper) throws ReflectiveOperationException {
		var clazz = t0.getClass();
		@SuppressWarnings("unchecked")
		var t1 = (T) Read.from(clazz.getConstructors()).uniqueResult().newInstance();
		for (var field : fields(clazz))
			try {
				var v0 = field.get(t0);
				var v1 = mapper.apply(v0);
				field.set(t1, v1);
			} catch (Exception ex) {
				throw new RuntimeException("in field " + clazz.getSimpleName() + "." + field.getName(), ex);
			}
		return t1;
	}

}
