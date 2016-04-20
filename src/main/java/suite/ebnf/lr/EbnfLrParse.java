package suite.ebnf.lr;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import suite.adt.Pair;
import suite.ebnf.Ebnf.Ast;
import suite.ebnf.EbnfGrammar;
import suite.ebnf.lr.Lr.Reduce;
import suite.ebnf.lr.Lr.State;
import suite.immutable.IList;
import suite.parser.Lexer;
import suite.streamlet.Read;
import suite.util.FunUtil.Source;
import suite.util.Util;

public class EbnfLrParse {

	private String rootEntity;
	private Lr lr;

	public static EbnfLrParse of(String grammar, String rootEntity) {
		try (StringReader reader = new StringReader(grammar)) {
			return new EbnfLrParse(EbnfGrammar.parse(reader), rootEntity);
		}
	}

	public EbnfLrParse(Map<String, EbnfGrammar> grammarByEntity, String rootEntity) {
		this.rootEntity = rootEntity;
		lr = new Lr(grammarByEntity, rootEntity);
	}

	public Ast check(String in) {
		return parse(in);
	}

	public Ast parse(String in) {
		Source<Ast> source = Read.from(new Lexer(in).tokens()).map(token -> new Ast(token, 0)).source();

		System.out.println("shifts/reduces = " + list(lr.fsm));
		System.out.println("Initial state = " + lr.state0);

		return parse(source, lr.state0);
	}

	private Ast parse(Source<Ast> tokens, State state) {
		Deque<Pair<Ast, State>> stack = new ArrayDeque<>();
		Ast token = tokens.source();

		while (true) {
			String lookahead = token != null ? token.entity : "EOF";
			Pair<State, Reduce> sr = shift(stack, state, lookahead);

			if (sr.t0 != null) { // Shift
				stack.push(Pair.of(token, state));
				state = sr.t0;
				token = tokens.source();
			} else { // Reduce
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

				// Force shift after reduce
				stack.push(Pair.of(token1, state));
				state = shift(stack, state, token1.entity).t0;
			}
		}
	}

	private Pair<State, Reduce> shift(Deque<Pair<Ast, State>> stack, State state, String next) {
		System.out.print("(S=" + state + ", Next=" + next + ", Stack=" + stack.size() + ")");
		Pair<State, Reduce> sr = lr.fsm.get(state).get(next);
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
