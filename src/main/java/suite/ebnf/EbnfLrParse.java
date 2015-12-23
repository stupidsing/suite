package suite.ebnf;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.ebnf.Ebnf.Node;
import suite.ebnf.EbnfGrammar.EbnfGrammarType;
import suite.immutable.IList;
import suite.parser.Lexer;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.util.FunUtil.Source;
import suite.util.Util;

public class EbnfLrParse {

	private int counter;
	private String rootEntity;
	private Map<String, Pair<State, State>> transitionByEntity = new HashMap<>();
	private Map<State, Map<String, State>> shifts = new HashMap<>();
	private Map<State, Reduce> reduces = new HashMap<>();
	private Map<State, Map<String, Pair<State, Reduce>>> fsm = new HashMap<>();

	private class Reduce {
		private String name;
		private int n;

		private Reduce(String name, int n) {
			this.name = name;
			this.n = n;
		}

		public String toString() {
			return name + "/" + n;
		}
	}

	private class State {
		private int id = counter++;

		public String toString() {
			return String.format("S%02d", id);
		}
	}

	public static EbnfLrParse of(String grammar, String rootEntity) {
		try (StringReader reader = new StringReader(grammar)) {
			EbnfGrammar eg0 = new EbnfGrammar(EbnfGrammarType.ENTITY, rootEntity);
			EbnfGrammar eg1 = new EbnfGrammar(EbnfGrammarType.ENTITY, "EOF");
			EbnfGrammar eg = new EbnfGrammar(EbnfGrammarType.AND___, Arrays.asList(eg0, eg1));
			Map<String, EbnfGrammar> egs = EbnfGrammar.parse(reader);
			egs.put("ROOT", new EbnfGrammar(EbnfGrammarType.NAMED_, "ROOT", eg));
			return new EbnfLrParse(egs, rootEntity);
		}
	}

	public EbnfLrParse(Map<String, EbnfGrammar> grammarByEntity, String rootEntity) {
		this.rootEntity = rootEntity;

		for (EbnfGrammar eg : grammarByEntity.values())
			buildLr(eg, new State());

		System.out.println("transitionByEntity = " + list(transitionByEntity));
		System.out.println("shifts = " + list(shifts));
		System.out.println("reduces = " + list(reduces));

		Read.from(shifts).sink((state0, map) -> {
			Read.from(map).sink((lookahead, statex) -> {
				put(subMap(fsm, state0), lookahead, Pair.of(statex, null));
			});
		});

		// State S -> Entity E
		// - Find all post-reduction states of entity E
		// - Merge the lookahead transitions into state S
		Streamlet2<State, State> ss0 = Read.from(shifts) //
				.concatMapValue(m -> Read.from(m.keySet())) //
				.mapValue(transitionByEntity::get) //
				.filter((state0, transition) -> transition != null) //
				.mapValue(Pair::first_);

		// Entity E -> State S
		// - Find all post-reduction states of entity E
		// - Merge the lookahead transitions of state S into those states
		Streamlet2<State, State> ss1 = Read.from(shifts) //
				.concatMap2((state0, m) -> Read.from(m)) //
				.mapKey(transitionByEntity::get) //
				.filter((transition, statex) -> transition != null) //
				.mapKey(Pair::second);

		// State S reduce to Entity E
		// - Find all possible states after entity E
		// - Merge those states into S as reduction lookahead
		ListMultimap<String, State> postReductionStatesByEntity = Read.from(shifts) //
				.concatMap2((state0, m) -> Read.from(m)) //
				.toMultimap();

		List<Pair<State, State>> mergeShifts = Streamlet2.concat(ss0, ss1).toList();

		List<Pair<State, State>> mergeReduces = Read.from(reduces) //
				.concatMapValue(reduce -> Read.from(postReductionStatesByEntity.get(reduce.name))) //
				.toList();

		System.out.println("mergeShifts = " + mergeShifts);
		System.out.println("mergeReduces = " + mergeReduces);

		while (true) {
			boolean b = false;

			for (Pair<State, State> pair : mergeShifts) {
				State sourceState = pair.t1;
				State targetState = pair.t0;
				System.out.println("Merging shifts from " + sourceState + " to " + targetState);
				Map<String, Pair<State, Reduce>> sourceMap = subMap(fsm, sourceState);
				Map<String, Pair<State, Reduce>> targetMap = subMap(fsm, targetState);
				for (Entry<String, Pair<State, Reduce>> e1 : sourceMap.entrySet())
					b |= resolve(targetMap, e1.getKey(), e1.getValue());
			}

			for (Pair<State, State> pair : mergeReduces) {
				State sourceState = pair.t1;
				State targetState = pair.t0;
				System.out.println("Merging reduces from " + sourceState + " to " + targetState);
				Map<String, Pair<State, Reduce>> sourceMap = subMap(fsm, sourceState);
				Map<String, Pair<State, Reduce>> targetMap = subMap(fsm, targetState);
				Reduce reduce = reduces.get(targetState);
				for (String lookahead : sourceMap.keySet())
					b |= resolve(targetMap, lookahead, Pair.of(null, reduce));
			}

			if (!b)
				break;
		}
	}

	public Node check(String in) {
		return parse(in);
	}

	public Node parse(String in) {
		Source<Node> source = Read.from(new Lexer(in).tokens()).map(token -> new Node(token, 0)).source();
		State state0 = transitionByEntity.get(rootEntity).t0;

		System.out.println("shifts/reduces = " + list(fsm));
		System.out.println("Initial state = " + state0);

		return parse(source, state0);
	}

	private Node parse(Source<Node> tokens, State state) {
		Deque<Pair<Node, State>> stack = new ArrayDeque<>();
		Node token = tokens.source();

		while (true) {
			String lookahead = token != null ? token.entity : "EOF";
			Pair<State, Reduce> sr = shift(stack, state, lookahead);

			if (sr.t0 != null) { // Shift
				stack.push(Pair.of(token, state));
				state = sr.t0;
				token = tokens.source();
			} else { // Reduce
				Reduce reduce = sr.t1;
				IList<Node> nodes = IList.end();

				for (int i = 0; i < reduce.n; i++) {
					Pair<Node, State> ns = stack.pop();
					nodes = IList.cons(ns.t0, nodes);
					state = ns.t1;
				}

				Node token1 = new Node(reduce.name, 0, 0, Read.from(nodes).toList());

				// Force shift after reduce
				if (rootEntity.equals(reduce.name) && stack.size() == 0 && token == null)
					return token1;

				stack.push(Pair.of(token1, state));
				state = shift(stack, state, token1.entity).t0;
			}
		}
	}

	private Pair<State, Reduce> shift(Deque<Pair<Node, State>> stack, State state, String lookahead) {
		System.out.print("(S=" + state + ", Lookahead=" + lookahead + ", Stack=" + stack.size() + ")");
		Pair<State, Reduce> sr = fsm.get(state).get(lookahead);
		System.out.println(" => " + sr);
		return sr;
	}

	private Pair<Integer, State> buildLr(EbnfGrammar eg, State state0) {
		Map<String, State> shiftMap = subMap(shifts, state0);

		BiConsumer<String, State> newEntity = (entity1, statex) -> {
			transitionByEntity.put(entity1, Pair.of(state0, statex));
			for (EbnfGrammar child : eg.children) {
				Pair<Integer, State> pair = buildLr(child, state0);
				Integer n = pair.t0;
				State state = pair.t1;
				put(reduces, state, new Reduce(entity1, n));
			}
		};

		int nTokens;
		State statex;

		switch (eg.type) {
		case AND___:
			nTokens = 0;
			statex = state0;
			for (EbnfGrammar child : eg.children) {
				Pair<Integer, State> pair = buildLr(child, statex);
				nTokens += pair.t0;
				statex = pair.t1;
			}
			break;
		case ENTITY:
			nTokens = 1;
			statex = shiftMap.computeIfAbsent(eg.content, content -> new State());
			break;
		case NAMED_:
			nTokens = 1;
			statex = new State();
			newEntity.accept(eg.content, statex);
			break;
		case OR____:
			String entity1 = "OR" + counter++;
			nTokens = 1;
			statex = shiftMap.computeIfAbsent(entity1, content -> new State());
			newEntity.accept(entity1, statex);
			break;
		case STRING:
			nTokens = 1;
			statex = shiftMap.computeIfAbsent(eg.content, content -> new State());
			break;
		default:
			throw new RuntimeException("LR parser cannot recognize " + eg.type);
		}

		return Pair.of(nTokens, statex);
	}

	private <K0, K1, V> Map<K1, V> subMap(Map<K0, Map<K1, V>> map, K0 k0) {
		return map.computeIfAbsent(k0, k -> new HashMap<>());
	}

	// Shift-reduce conflict ends in reduce
	private boolean resolve(Map<String, Pair<State, Reduce>> map, String key, Pair<State, Reduce> pair1) {
		Pair<State, Reduce> pair0 = map.get(key);
		if (pair0 == null || isShiftReduceConflict(pair0, pair1)) {
			map.put(key, pair1);
			return true;
		} else if (pair0.equals(pair1) || isShiftReduceConflict(pair1, pair0))
			return false;
		else
			throw new RuntimeException("Duplicate key " + key + " old (" + pair0 + ") new (" + pair1 + ")");
	}

	private boolean isShiftReduceConflict(Pair<State, Reduce> shift, Pair<State, Reduce> reduce) {
		return shift.t1 == null && reduce.t1 != null;
	}

	private <K, V> boolean put(Map<K, V> map, K key, V value1) {
		V value0 = map.get(key);
		if (value0 == null) {
			map.put(key, value1);
			return true;
		} else if (value0.equals(value1))
			return false;
		else
			throw new RuntimeException("Duplicate key " + key + " old (" + value0 + ") new (" + value1 + ")");
	}

	public <K, V> String list(Map<K, V> map) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		Read.from(map) //
				.mapKey(Object::toString) //
				.sortByKey(Util::compare) //
				.sink((k, v) -> sb.append(k + " = " + v + "\n"));
		sb.append("}\n");
		return sb.toString();
	}

}
