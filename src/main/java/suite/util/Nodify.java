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

	private Set<Type> collectionClasses = Set.of(ArrayList.class, Collection.class, HashSet.class, List.class, Set.class);
	private Set<Type> mapClasses = Set.of(HashMap.class, Map.class);

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
		var t = (T) apply_(getNodifier(clazz), node);
		return t;
	}

	private Nodifier getNodifier(Type type) {
		var nodifier = nodifiers.get(type);
		if (nodifier == null) {
			nodifiers.put(type, new Nodifier(o -> apply_(getNodifier(type), o), n -> apply_(getNodifier(type), n)));
			nodifiers.put(type, nodifier = newNodifier(type));
		}
		return nodifier;
	}

	@SuppressWarnings("unchecked")
	private Nodifier newNodifier(Type type) {
		return new Switch<Nodifier>(type //
		).applyIf(Class.class, clazz -> {
			if (clazz == boolean.class)
				return new Nodifier(o -> Atom.of(o.toString()), n -> n == Atom.TRUE);
			else if (clazz == int.class)
				return new Nodifier(o -> Int.of((Integer) o), n -> ((Int) n).number);
			else if (clazz == Chars.class)
				return new Nodifier(o -> new Str(o.toString()), n -> To.chars(((Str) n).value));
			else if (clazz == String.class)
				return new Nodifier(o -> new Str(o.toString()), n -> ((Str) n).value);
			else if (clazz.isEnum())
				return new Nodifier(o -> Atom.of(o.toString()),
						Read.from(clazz.getEnumConstants()).toMap(e -> Atom.of(e.toString()))::get);
			else if (clazz.isArray()) {
				var componentType = clazz.getComponentType();
				var nodifier1 = getNodifier(componentType);
				Fun<Object, Node> forward = o -> {
					Node node = Atom.NIL;
					for (var i = Array.getLength(o) - 1; 0 <= i; i--)
						node = Tree.of(TermOp.OR____, apply_(nodifier1, Array.get(o, i)), node);
					return node;
				};
				return new Nodifier(forward, n -> {
					var list = Read //
							.from(Tree.iter(n, TermOp.OR____)) //
							.map(n_ -> apply_(nodifier1, n_)) //
							.toList();
					return To.array_(list.size(), componentType, list::get);
				});
			} else if (clazz.isInterface()) // polymorphism
				return new Nodifier(o -> {
					var clazz1 = o.getClass();
					var n = apply_(getNodifier(clazz1), o);
					return Tree.of(TermOp.COLON_, Atom.of(clazz1.getName()), n);
				}, n -> {
					var tree = Tree.decompose(n, TermOp.COLON_);
					if (tree != null) {
						var clazz1 = Rethrow.ex(() -> Class.forName(((Atom) tree.getLeft()).name));
						return apply_(getNodifier(clazz1), tree.getRight());
					} else
						// happens when an enum implements an interface
						return Fail.t("cannot instantiate enum from interfaces");
				});
			else {
				var fieldInfos = Read //
						.from(inspect.fields(clazz)) //
						.map(field -> new FieldInfo(field, field.getName(), getNodifier(field.getGenericType()))) //
						.toList();

				var pairs = Read.from(fieldInfos).map(f -> Pair.of(Atom.of(f.name), f)).toList();

				return new Nodifier(o -> Rethrow.ex(() -> {
					var dict = Dict.of();
					for (var pair : pairs) {
						var fieldInfo = pair.t1;
						var value = apply_(fieldInfo.nodifier, fieldInfo.field.get(o));
						dict.map.put(pair.t0, Reference.of(value));
					}
					return dict;
				}), n -> Rethrow.ex(() -> {
					var map = ((Dict) n).map;
					var o1 = Object_.new_(clazz);
					for (var pair : pairs) {
						var fieldInfo = pair.t1;
						var value = map.get(pair.t0).finalNode();
						fieldInfo.field.set(o1, apply_(fieldInfo.nodifier, value));
					}
					return o1;
				}));
			}
		}).applyIf(ParameterizedType.class, pt -> {
			var rawType = pt.getRawType();
			var typeArgs = pt.getActualTypeArguments();
			var clazz = rawType instanceof Class ? (Class<?>) rawType : null;

			if (collectionClasses.contains(clazz)) {
				var nodifier1 = getNodifier(typeArgs[0]);
				return new Nodifier(o -> {
					Tree start = Tree.of(null, null, null), tree = start;
					for (var o_ : (Collection<?>) o) {
						var tree0 = tree;
						Tree.forceSetRight(tree0, tree = Tree.of(TermOp.OR____, apply_(nodifier1, o_), null));
					}
					Tree.forceSetRight(tree, Atom.NIL);
					return start.getRight();
				}, n -> {
					var list = Read.from(Tree.iter(n, TermOp.OR____)).map(n_ -> apply_(nodifier1, n_)).toList();
					var o1 = (Collection<Object>) Object_.instantiate(clazz);
					o1.addAll(list);
					return o1;
				});
			} else if (mapClasses.contains(clazz)) {
				var kn = getNodifier(typeArgs[0]);
				var vn = getNodifier(typeArgs[1]);
				return new Nodifier(o -> {
					var dict = Dict.of();
					for (var e : ((Map<?, ?>) o).entrySet())
						dict.map.put(apply_(kn, e.getKey()), Reference.of(apply_(vn, e.getValue())));
					return dict;
				}, n -> {
					var map = ((Dict) n).map;
					var object1 = (Map<Object, Object>) Object_.instantiate(clazz);
					for (var e : map.entrySet())
						object1.put(apply_(kn, e.getKey()), apply_(vn, e.getValue().finalNode()));
					return object1;
				});
			} else
				return getNodifier(rawType);
		}).nonNullResult();
	}

	private Node apply_(Nodifier nodifier, Object object) {
		return object != null ? nodifier.nodify.apply(object) : Atom.NULL;
	}

	private Object apply_(Nodifier nodifier, Node node) {
		return node != Atom.NULL ? nodifier.unnodify.apply(node) : null;
	}

}
