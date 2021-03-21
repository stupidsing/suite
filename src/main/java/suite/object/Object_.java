package suite.object;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import primal.Verbs.New;
import primal.adt.Pair;
import primal.fp.Funs.Iterate;
import suite.node.util.Singleton;
import suite.util.Util;

public class Object_ {

	public static class Mapper {
		private Iterate<Object> map;
		private Iterate<Object> unmap;

		private Mapper(Iterate<Object> map, Iterate<Object> unmap) {
			this.map = map;
			this.unmap = unmap;
		}
	}

	public static Mapper mapper(Type type) {
		Mapper mapper;

		if (type instanceof Class clazz) {
			if (Util.isSimple(clazz))
				mapper = new Mapper(object -> object, object -> object);
			else if (clazz.isArray()) {
				var componentType = clazz.getComponentType();

				mapper = new Mapper(object -> {
					var map = new HashMap<>();
					var length = Array.getLength(object);
					for (var i = 0; i < length; i++)
						map.put(i, Array.get(object, i));
					return map;
				}, object -> {
					var map = (Map<?, ?>) object;
					var objects = Array.newInstance(componentType, map.size());
					var i = 0;
					while (map.containsKey(i)) {
						Array.set(objects, i, map.get(i));
						i++;
					}
					return objects;
				});
			} else if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) // polymorphism
				mapper = new Mapper(object -> {
					var clazz1 = object.getClass();
					var m = apply_(mapper(clazz1).map, object);
					if (m instanceof Map) {
						@SuppressWarnings("unchecked")
						var map = (Map<String, String>) m;
						map.put("@class", clazz1.getName());
						return map;
					} else
						// happens when an enum implements an interface
						return m;
				}, object -> {
					if (object instanceof Map<?, ?> map) {
						var className = map.get("@class").toString();
						var clazz1 = ex(() -> Class.forName(className));
						return apply_(mapper(clazz1).unmap, object);
					} else
						// happens when an enum implements an interface
						return object;
				});
			else {
				var inspect = Singleton.me.inspect;

				var sfs = inspect //
						.fields(clazz) //
						.map(field -> Pair.of(field.getName(), field)) //
						.toList();

				mapper = new Mapper(object -> ex(() -> {
					var map = new HashMap<>();
					for (var sf : sfs)
						map.put(sf.k, sf.v.get(object));
					return map;
				}), object -> ex(() -> {
					var map = (Map<?, ?>) object;
					var object1 = New.clazz(clazz);
					for (var sf : sfs)
						sf.v.set(object1, map.get(sf.k));
					return object1;
				}));
			}
		} else if (type instanceof ParameterizedType pt) {
			var rawType = pt.getRawType();
			var clazz = rawType instanceof Class<?> c ? c : null;

			if (List.class.isAssignableFrom(clazz))
				mapper = new Mapper(object -> {
					var map = new HashMap<>();
					var i = 0;
					for (var o : (Collection<?>) object)
						map.put(i++, o);
					return map;
				}, object -> {
					var map = (Map<?, ?>) object;
					var object1 = new ArrayList<>();
					var i = 0;
					while (map.containsKey(i))
						object1.add(map.get(i++));
					return object1;
				});
			else if (Map.class.isAssignableFrom(clazz))
				mapper = new Mapper(object -> object, object -> object);
			else
				mapper = mapper(rawType);
		} else
			mapper = fail("unrecognized type " + type);

		return mapper;
	}

	private static Object apply_(Iterate<Object> fun, Object object) {
		return object != null ? fun.apply(object) : null;
	}

}
