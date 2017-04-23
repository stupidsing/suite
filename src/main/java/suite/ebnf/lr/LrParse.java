package suite.ebnf.lr;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import suite.adt.Pair;
import suite.ebnf.Ebnf.Ast;
import suite.ebnf.Grammar;
import suite.ebnf.lr.BuildLr.Reduce;
import suite.ebnf.lr.BuildLr.State;
import suite.immutable.IList;
import suite.parser.Lexer;
import suite.streamlet.Read;
import suite.util.FunUtil.Source;
import suite.util.Util;

public class LrParse {

	private String rootEntity;
	private BuildLr buildLr;

	public static LrParse of(String grammar, String rootEntity) {
		try (StringReader reader = new StringReader(grammar)) {
			return new LrParse(Grammar.parse(reader), rootEntity);
		}
	}

	public LrParse(Map<String, Grammar> grammarByEntity, String rootEntity) {
		this.rootEntity = rootEntity;
		buildLr = new BuildLr(grammarByEntity, rootEntity);
	}

	public Ast check(String in) {
		return parse(in);
	}

	public Ast parse(String in) {
		Source<Ast> source = Read.from(() -> new Lexer(in).tokens()).map(token -> new Ast(token, 0)).source();

		System.out.println("shifts/reduces = " + list(buildLr.fsm));
		System.out.println("Initial state = " + buildLr.state0);

		return parse(source, buildLr.state0);
	}

	private Ast parse(Source<Ast> tokens, State state) {
		Deque<Pair<Ast, State>> stack = new ArrayDeque<>();
		Ast token = tokens.source();

		while (true) {
			String lookahead = token != null ? token.entity : "EOF";
			Pair<State, Reduce> sr = shift(stack, state, lookahead);

			if (sr.t0 != null) { // shift
				stack.push(Pair.of(token, state));
				state = sr.t0;
				token = tokens.source();
			} else { // reduce
				Reduce reduce = sr.t1;
				IList<Ast> nodes = IList.end();

				for (int i = 0; i < reduce.n(); i++) {
					Pair<Ast, State> ns = stack.pop();
					nodes = IList.cons(ns.t0, nodes);
					state = ns.t1;
				}

				Ast token1 = new Ast(reduce.name(), 0, 0, Read.from(nodes).toList());

				if (rootEntity.equals(reduce.name()) && stack.size() == 0 && token == null)
					return token1;

				// force shift after reduce
				stack.push(Pair.of(token1, state));
				state = shift(stack, state, token1.entity).t0;
			}
		}
	}

	private Pair<State, Reduce> shift(Deque<Pair<Ast, State>> stack, State state, String next) {
		System.out.print("(S=" + state + ", Next=" + next + ", Stack=" + stack.size() + ")");
		Pair<State, Reduce> sr = buildLr.fsm.get(state).get(next);
		System.out.println(" => " + sr);
		return sr;
	}

	private <K, V> String list(Map<K, V> map) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		Read.from2(map) //
				.mapKey(Object::toString) //
				.sortByKey(Util::compare) //
				.sink((k, v) -> sb.append(k + " = " + v + "\n"));
		sb.append("}\n");
		return sb.toString();
	}

}
