package suite.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import suite.adt.Pair;
import suite.inspect.Inspect;
import suite.node.Atom;
import suite.node.Dict;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.primitive.Chars;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;

/**
 * Convert (supposedly) any Java structures to nodes.
 *
 * @author ywsing
 */
public class Nodify {

	private Set<Class<?>> collectionClasses = new HashSet<>(Arrays.asList( //
			ArrayList.class, Collection.class, HashSet.class, List.class, Set.class));
	private Set<Class<?>> mapClasses = new HashSet<>(Arrays.asList( //
			HashMap.class, Map.class));

	private Map<Class<?>, Fun<Object, Node>> nodifiers = new ConcurrentHashMap<>();
	private Map<Class<?>, Fun<Node, Object>> unnodifiers = new ConcurrentHashMap<>();

	private Inspect inspect;

	private Atom CLASS = Atom.of("@class");
	private Atom NULL = Atom.of("null");
	private Atom TRUE = Atom.of("true");

	private class FieldInfo {
		private Field field;
		private String name;
		private Fun<Object, Node> nodifier;
		private Fun<Node, Object> unnodifier;

		private FieldInfo(Field field, String name, Fun<Object, Node> nodifier, Fun<Node, Object> unnodifier) {
			this.field = field;
			this.name = name;
			this.nodifier = nodifier;
			this.unnodifier = unnodifier;
		}
	}

	public Nodify(Inspect inspect) {
		this.inspect = inspect;
	}

	public <T> Node nodify(Class<T> clazz, T t) {
		if (t != null)
			return getNodifier(clazz).apply(t);
		else
			return null;
	}

	public <T> T unnodify(Class<T> clazz, Node node) {
		if (node != NULL) {
			@SuppressWarnings("unchecked")
			T t = (T) getUnnodifier(clazz).apply(node);
			return t;
		} else
			return null;
	}

	private Fun<Object, Node> getNodifier(Class<?> clazz) {
		Fun<Object, Node> nodifier = nodifiers.get(clazz);
		if (nodifier == null) {
			nodifiers.put(clazz, object -> getNodifier(clazz).apply(object));
			nodifiers.put(clazz, nodifier = createNodifier0(clazz));
		}
		return nodifier;
	}

	private Fun<Node, Object> getUnnodifier(Class<?> clazz) {
		Fun<Node, Object> unnodifier = unnodifiers.get(clazz);
		if (unnodifier == null) {
			unnodifiers.put(clazz, node -> getUnnodifier(clazz).apply(node));
			unnodifiers.put(clazz, unnodifier = createUnnodifier0(clazz));
		}
		return unnodifier;
	}

	private Fun<Object, Node> createNodifier0(Type type) {
		if (type instanceof Class) {
			Class<?> clazz = (Class<?>) type;

			if (clazz == boolean.class)
				return object -> Atom.of(object.toString());
			else if (clazz == int.class)
				return object -> Int.of((Integer) object);
			else if (clazz == Chars.class || clazz == String.class)
				return object -> new Str(object.toString());
			else if (clazz.isEnum())
				return object -> Atom.of(object.toString());
			else if (clazz.isArray()) {
				Class<?> componentType = clazz.getComponentType();
				Fun<Object, Node> nodifier1 = createNodifier0(componentType);
				if (componentType.isPrimitive())
					return object -> {
						Node node = Atom.NIL;
						for (int i = Array.getLength(object) - 1; i >= 0; i--)
							node = Tree.of(TermOp.OR____, apply0(nodifier1, Array.get(object, i)), node);
						return node;
					};
				else
					return object -> {
						Node node = Atom.NIL;
						Object objects[] = (Object[]) object;
						for (int i = objects.length - 1; i >= 0; i--)
							node = Tree.of(TermOp.OR____, apply0(nodifier1, objects[i]), node);
						return node;
					};
			} else if (clazz.isInterface()) // Polymorphism
				return object -> {
					Class<?> clazz1 = object.getClass();
					Node d = getNodifier(clazz1).apply(object);
					if (d instanceof Dict)
						((Dict) d).map.put(CLASS, Reference.of(new Str(clazz1.getName())));
					return d;
				};
			else {
				List<FieldInfo> fieldInfos = getFieldInfos(clazz);
				return object -> {
					Dict dict = new Dict();
					for (FieldInfo fieldInfo : fieldInfos)
						try {
							Node value = apply0(fieldInfo.nodifier, fieldInfo.field.get(object));
							dict.map.put(Atom.of(fieldInfo.name), Reference.of(value));
						} catch (ReflectiveOperationException ex) {
							throw new RuntimeException(ex);
						}
					return dict;
				};
			}
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			Type rawType = pt.getRawType();
			Type typeArguments[] = pt.getActualTypeArguments();
			Class<?> clazz = rawType instanceof Class ? (Class<?>) rawType : null;

			if (collectionClasses.contains(clazz)) {
				Fun<Object, Node> nodifier1 = createNodifier0(typeArguments[0]);
				return object -> {
					Tree start = Tree.of(null, null, null), tree = start;
					for (Object o : (Collection<?>) object) {
						Tree tree0 = tree;
						Tree.forceSetRight(tree0, tree = Tree.of(TermOp.OR____, apply0(nodifier1, o), null));
					}
					Tree.forceSetRight(tree, Atom.NIL);
					return start.getRight();
				};
			} else if (mapClasses.contains(clazz)) {
				Fun<Object, Node> keyNodifier = createNodifier0(typeArguments[0]);
				Fun<Object, Node> valueNodifier = createNodifier0(typeArguments[1]);
				return object -> {
					Dict dict = new Dict();
					for (Entry<?, ?> e : ((Map<?, ?>) object).entrySet())
						dict.map.put(apply0(keyNodifier, e.getKey()), Reference.of(apply0(valueNodifier, e.getValue())));
					return dict;
				};
			} else
				return createNodifier0(rawType);
		}

		throw new RuntimeException("Unrecognized type " + type);
	}

	private Fun<Node, Object> createUnnodifier0(Type type) {
		if (type instanceof Class) {
			Class<?> clazz = (Class<?>) type;

			if (clazz == boolean.class)
				return node -> node == TRUE;
			else if (clazz == int.class)
				return node -> ((Int) node).number;
			else if (clazz == Chars.class || clazz == String.class)
				return node -> ((Str) node).value;
			else if (clazz.isEnum())
				return Read.from(clazz.getEnumConstants()).toMap(e -> Atom.of(e.toString()), e -> e)::get;
			else if (clazz.isArray()) {
				Class<?> componentType = clazz.getComponentType();
				Fun<Node, Object> unnodifier1 = createUnnodifier0(componentType);
				return node -> {
					List<Object> list = Read.from(To.source(Tree.iter(node, TermOp.OR____))).map(unnodifier1::apply).toList();
					if (componentType.isPrimitive()) {
						int size = list.size();
						Object objects = Array.newInstance(componentType, size);
						for (int i = 0; i < size; i++)
							Array.set(objects, i, list.get(i));
						return objects;
					} else
						return list.toArray();
				};
			} else if (clazz.isInterface()) // Polymorphism
				return node -> {
					if (node instanceof Dict) {
						Map<?, ?> map = (Map<?, ?>) node;
						Class<?> clazz1;
						try {
							clazz1 = Class.forName(map.get("@class").toString());
						} catch (ClassNotFoundException ex) {
							throw new RuntimeException(ex);
						}
						return getUnnodifier(clazz1).apply(node);
					} else
						// Happens when an enum implements an interface
						throw new RuntimeException("Cannot instantiate enum from interfaces");
				};
			else {
				List<Pair<Atom, FieldInfo>> pairs = Read.from(getFieldInfos(clazz)).map(f -> Pair.of(Atom.of(f.name), f)).toList();
				return node -> {
					Map<Node, Reference> map = ((Dict) node).map;
					try {
						Object object1 = clazz.newInstance();
						for (Pair<Atom, FieldInfo> pair : pairs) {
							FieldInfo fieldInfo = pair.t1;
							fieldInfo.field.set(object1, apply0(fieldInfo.unnodifier, map.get(pair.t0)));
						}
						return object1;
					} catch (ReflectiveOperationException ex) {
						throw new RuntimeException(ex);
					}
				};
			}
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			Type rawType = pt.getRawType();
			Type typeArguments[] = pt.getActualTypeArguments();
			Class<?> clazz = rawType instanceof Class ? (Class<?>) rawType : null;

			if (collectionClasses.contains(clazz)) {
				Fun<Node, Object> unnodifier1 = createUnnodifier0(typeArguments[0]);
				return node -> {
					List<Object> list = Read.from(To.source(Tree.iter(node, TermOp.OR____))).map(unnodifier1::apply).toList();
					@SuppressWarnings("unchecked")
					Collection<Object> object1 = (Collection<Object>) create(clazz);
					object1.addAll(list);
					return object1;
				};
			} else if (mapClasses.contains(clazz)) {
				Fun<Node, Object> keyUnnodifier = createUnnodifier0(typeArguments[0]);
				Fun<Node, Object> valueUnnodifier = createUnnodifier0(typeArguments[1]);
				return node -> {
					Map<Node, Reference> map = ((Dict) node).map;
					@SuppressWarnings("unchecked")
					Map<Object, Object> object1 = (Map<Object, Object>) create(clazz);
					for (Entry<Node, Reference> e : map.entrySet())
						object1.put(apply0(keyUnnodifier, e.getKey()), apply0(valueUnnodifier, e.getValue()));
					return object1;
				};
			} else
				return createUnnodifier0(rawType);
		}

		throw new RuntimeException("Unrecognized type " + type);
	}

	private <T> T create(Class<T> clazz) {
		try {
			Object object;
			if (clazz == ArrayList.class || clazz == Collection.class || clazz == List.class)
				object = new ArrayList<>();
			else if (clazz == HashSet.class || clazz == Set.class)
				object = new HashSet<>();
			else if (clazz == HashMap.class || clazz == Map.class)
				object = new HashMap<>();
			else
				return clazz.newInstance();

			@SuppressWarnings("unchecked")
			T t = (T) object;
			return t;
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException(ex);
		}
	}

	private List<FieldInfo> getFieldInfos(Class<?> clazz) {
		return Read.from(inspect.fields(clazz)) //
				.map(field -> {
					Type type = field.getGenericType();
					return new FieldInfo(field, field.getName(), createNodifier0(type), createUnnodifier0(type));
				}) //
				.toList();
	}

	private Node apply0(Fun<Object, Node> fun, Object object) {
		return object != null ? fun.apply(object) : NULL;
	}

	private Object apply0(Fun<Node, Object> fun, Node node) {
		return node != null ? fun.apply(node) : null;
	}

}
