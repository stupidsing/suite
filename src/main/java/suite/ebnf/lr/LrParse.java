package suite.ebnf.lr;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import primal.MoreVerbs.Read;
import primal.Verbs.Compare;
import primal.adt.Pair;
import primal.persistent.PerList;
import primal.puller.Puller;
import suite.ebnf.Ebnf.Ast;
import suite.ebnf.Grammar;
import suite.ebnf.lr.BuildLr.Reduce;
import suite.ebnf.lr.BuildLr.State;
import suite.parser.Lexer;

public class LrParse {

	private String rootEntity;
	private BuildLr buildLr;

	public static LrParse of(String grammar, String rootEntity) {
		try (var reader = new StringReader(grammar)) {
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
		var puller = new Lexer(in).tokens().map(Ast::new);

		System.out.println("shifts/reduces = " + list(buildLr.fsm));
		System.out.println("Initial state = " + buildLr.state0);

		return parse(puller, buildLr.state0);
	}

	private Ast parse(Puller<Ast> tokens, State state) {
		var stack = new ArrayDeque<Pair<Ast, State>>();
		var token = tokens.pull();

		while (true) {
			var lookahead = token != null ? token.entity : "EOF";
			var sr = shift(stack, state, lookahead);

			if (sr.k != null) { // shift
				stack.push(Pair.of(token, state));
				state = sr.k;
				token = tokens.pull();
			} else { // reduce
				var reduce = sr.v;
				var nodes = PerList.<Ast> end();

				for (var i = 0; i < reduce.n(); i++) {
					var ns = stack.pop();
					nodes = PerList.cons(ns.k, nodes);
					state = ns.v;
				}

				var name = reduce.name();
				var token1 = new Ast(name, 0, 0, nodes.streamlet().toList());

				if (rootEntity.equals(name) && stack.size() == 0 && token == null)
					return token1;

				// force shift after reduce
				stack.push(Pair.of(token1, state));
				state = shift(stack, state, token1.entity).k;
			}
		}
	}

	private Pair<State, Reduce> shift(Deque<Pair<Ast, State>> stack, State state, String next) {
		System.out.print("(S=" + state + ", Next=" + next + ", Stack=" + stack.size() + ")");
		var sr = buildLr.fsm.get(state).get(next);
		System.out.println(" => " + sr);
		return sr;
	}

	private <K, V> String list(Map<K, V> map) {
		return Read //
				.from2(map) //
				.map2((k, v) -> k.toString(), (k, v) -> v) //
				.sortByKey(Compare::objects) //
				.map((k, v) -> k + " = " + v + "\n") //
				.toJoinedString("{\n", "", "}\n");
	}

}
