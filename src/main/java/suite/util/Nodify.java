package suite.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import suite.adt.pair.Pair;
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

	private Set<Type> collectionClasses = To.set(ArrayList.class, Collection.class, HashSet.class, List.class, Set.class);
	private Set<Type> mapClasses = To.set(HashMap.class, Map.class);

	private Map<Type, Nodifier> nodifiers = new ConcurrentHashMap<>();
	private Inspect inspect;

	private class FieldInfo {
		private Field field;
		private String name;
		private Nodifier nodifier;

		private FieldInfo(Field field, String name, Nodifier nodifier) {
			this.field = field;
			this.name = name;
			this.nodifier = nodifier;
		}
	}

	private class Nodifier {
		private Fun<Object, Node> nodify;
		private Fun<Node, Object> unnodify;

		private Nodifier(Fun<Object, Node> nodify, Fun<Node, Object> unnodify) {
			this.nodify = nodify;
			this.unnodify = unnodify;
		}
	}

	public Nodify(Inspect inspect) {
		this.inspect = inspect;
	}

	public <T> Node nodify(Class<T> clazz, T t) {
		return apply_(getNodifier(clazz), t);
	}

	public <T> T unnodify(Class<T> clazz, Node node) {
		@SuppressWarnings("unchecked")
		T t = (T) apply_(getNodifier(clazz), node);
		return t;
	}

	private Nodifier getNodifier(Type type) {
		Nodifier nodifier = nodifiers.get(type);
		if (nodifier == null) {
			nodifiers.put(type, new Nodifier(object -> apply_(getNodifier(type), object), node -> apply_(getNodifier(type), node)));
			nodifiers.put(type, nodifier = newNodifier(type));
		}
		return nodifier;
	}

	@SuppressWarnings("unchecked")
	private Nodifier newNodifier(Type type) {
		Nodifier nodifier;

		if (type instanceof Class) {
			Class<?> clazz = (Class<?>) type;

			if (clazz == boolean.class)
				nodifier = new Nodifier(object -> Atom.of(object.toString()), node -> node == Atom.TRUE);
			else if (clazz == int.class)
				nodifier = new Nodifier(object -> Int.of((Integer) object), node -> ((Int) node).number);
			else if (clazz == Chars.class)
				nodifier = new Nodifier(object -> new Str(object.toString()), node -> To.chars(((Str) node).value));
			else if (clazz == String.class)
				nodifier = new Nodifier(object -> new Str(object.toString()), node -> ((Str) node).value);
			else if (clazz.isEnum())
				nodifier = new Nodifier(object -> Atom.of(object.toString()),
						Read.from(clazz.getEnumConstants()).toMap(e -> Atom.of(e.toString()))::get);
			else if (clazz.isArray()) {
				Class<?> componentType = clazz.getComponentType();
				Nodifier nodifier1 = getNodifier(componentType);
				Fun<Object, Node> forward = object -> {
					Node node = Atom.NIL;
					for (var i = Array.getLength(object) - 1; 0 <= i; i--)
						node = Tree.of(TermOp.OR____, apply_(nodifier1, Array.get(object, i)), node);
					return node;
				};
				nodifier = new Nodifier(forward, node -> {
					List<Object> list = Read //
							.from(Tree.iter(node, TermOp.OR____)) //
							.map(n -> apply_(nodifier1, n)) //
							.toList();
					var size = list.size();
					Object objects = Array.newInstance(componentType, size);
					for (var i = 0; i < size; i++)
						Array.set(objects, i, list.get(i));
					return objects;
				});
			} else if (clazz.isInterface()) // polymorphism
				nodifier = new Nodifier(object -> {
					Class<?> clazz1 = object.getClass();
					Node n = apply_(getNodifier(clazz1), object);
					return Tree.of(TermOp.COLON_, Atom.of(clazz1.getName()), n);
				}, node -> {
					Tree tree = Tree.decompose(node, TermOp.COLON_);
					if (tree != null) {
						Class<?> clazz1;
						try {
							clazz1 = Class.forName(((Atom) tree.getLeft()).name);
						} catch (ClassNotFoundException ex) {
							clazz1 = Fail.t(ex);
						}
						return apply_(getNodifier(clazz1), tree.getRight());
					} else
						// happens when an enum implements an interface
						return Fail.t("cannot instantiate enum from interfaces");
				});
			else {
				List<FieldInfo> fieldInfos = Read //
						.from(inspect.fields(clazz)) //
						.map(field -> {
							Type type1 = field.getGenericType();
							return new FieldInfo(field, field.getName(), getNodifier(type1));
						}) //
						.toList();

				List<Pair<Atom, FieldInfo>> pairs = Read.from(fieldInfos).map(f -> Pair.of(Atom.of(f.name), f)).toList();
				nodifier = new Nodifier(object -> Rethrow.ex(() -> {
					Dict dict = new Dict();
					for (Pair<Atom, FieldInfo> pair : pairs) {
						FieldInfo fieldInfo = pair.t1;
						Node value = apply_(fieldInfo.nodifier, fieldInfo.field.get(object));
						dict.map.put(pair.t0, Reference.of(value));
					}
					return dict;
				}), node -> Rethrow.ex(() -> {
					Map<Node, Reference> map = ((Dict) node).map;
					var object1 = Object_.new_(clazz);
					for (Pair<Atom, FieldInfo> pair : pairs) {
						FieldInfo fieldInfo = pair.t1;
						var value = map.get(pair.t0).finalNode();
						fieldInfo.field.set(object1, apply_(fieldInfo.nodifier, value));
					}
					return object1;
				}));
			}
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			Type rawType = pt.getRawType();
			Type[] typeArgs = pt.getActualTypeArguments();
			Class<?> clazz = rawType instanceof Class ? (Class<?>) rawType : null;

			if (collectionClasses.contains(clazz)) {
				Nodifier nodifier1 = getNodifier(typeArgs[0]);
				nodifier = new Nodifier(object -> {
					Tree start = Tree.of(null, null, null), tree = start;
					for (Object o : (Collection<?>) object) {
						Tree tree0 = tree;
						Tree.forceSetRight(tree0, tree = Tree.of(TermOp.OR____, apply_(nodifier1, o), null));
					}
					Tree.forceSetRight(tree, Atom.NIL);
					return start.getRight();
				}, node -> {
					List<Object> list = Read.from(Tree.iter(node, TermOp.OR____)).map(n -> apply_(nodifier1, n)).toList();
					Collection<Object> object1 = (Collection<Object>) instantiate(clazz);
					object1.addAll(list);
					return object1;
				});
			} else if (mapClasses.contains(clazz)) {
				Nodifier kn = getNodifier(typeArgs[0]);
				Nodifier vn = getNodifier(typeArgs[1]);
				nodifier = new Nodifier(object -> {
					Dict dict = new Dict();
					for (Entry<?, ?> e : ((Map<?, ?>) object).entrySet())
						dict.map.put(apply_(kn, e.getKey()), Reference.of(apply_(vn, e.getValue())));
					return dict;
				}, node -> {
					Map<Node, Reference> map = ((Dict) node).map;
					Map<Object, Object> object1 = (Map<Object, Object>) instantiate(clazz);
					for (var e : map.entrySet())
						object1.put(apply_(kn, e.getKey()), apply_(vn, e.getValue().finalNode()));
					return object1;
				});
			} else
				nodifier = getNodifier(rawType);
		} else
			nodifier = Fail.t("unrecognized type " + type);

		return nodifier;
	}

	private <T> T instantiate(Class<T> clazz) {
		Object object;
		if (clazz == ArrayList.class || clazz == Collection.class || clazz == List.class)
			object = new ArrayList<>();
		else if (clazz == HashSet.class || clazz == Set.class)
			object = new HashSet<>();
		else if (clazz == HashMap.class || clazz == Map.class)
			object = new HashMap<>();
		else
			return Object_.new_(clazz);

		@SuppressWarnings("unchecked")
		T t = (T) object;
		return t;
	}

	private Node apply_(Nodifier nodifier, Object object) {
		return object != null ? nodifier.nodify.apply(object) : Atom.NULL;
	}

	private Object apply_(Nodifier nodifier, Node node) {
		return node != Atom.NULL ? nodifier.unnodify.apply(node) : null;
	}

}
