package suite.ebnf;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import primal.primitive.adt.Chars;
import suite.ebnf.topdown.ReduceHeadRecursion;
import suite.ebnf.topdown.TopDownParse;
import suite.node.parser.FactorizeResult;
import suite.node.parser.FactorizeResult.FTerminal;

/**
 * Parser for Backus-Naur form grammars.
 *
 * TODO cyclic problem:
 *
 * primary-no-array-creation-expression ::= simple-name | member-access
 *
 * member-access ::= primary-expression "." identifier (type-argument-list)?
 *
 * primary-expression ::= primary-no-array-creation-expression
 *
 * @author ywsing
 */
public class Ebnf {

	private TopDownParse engine;

	public static class Ast {
		public int start, end;
		public final String entity;
		public final List<Ast> children;

		public Ast(String entity) {
			this(entity, 0);
		}

		public Ast(String entity, int start) {
			this(entity, start, 0, new ArrayList<>());
		}

		public Ast(String entity, int start, int end, List<Ast> children) {
			this.entity = entity;
			this.start = start;
			this.end = end;
			this.children = children;
		}

		public String toString() {
			return entity + "@" + start + "-" + end + children;
		}

		public int getStart() {
			return start;
		}

		public int getEnd() {
			return end;
		}
	}

	public Ebnf(Reader reader) throws IOException {
		var grammarByEntity = Grammar.parse(reader);
		var rhr = new ReduceHeadRecursion(grammarByEntity);

		for (var e : grammarByEntity.entrySet())
			e.setValue(rhr.reduce(e.getValue()));

		engine = new TopDownParse(grammarByEntity);
	}

	public FactorizeResult parseFNode(String s, String entity) {
		var cs = s.toCharArray();
		return toFactorizeResult(cs, 0, cs.length, parse(entity, s));
	}

	private FactorizeResult toFactorizeResult(char[] cs, int p0, int px, Ast ast) {
		var children = ast.children;
		var size = children.size();

		if (0 < size) {
			var frs = new ArrayList<FactorizeResult>();
			var pos = p0;
			for (var i = 0; i < size; i++) {
				var child = children.get(i);
				var pos0 = pos;
				pos = i != size - 1 ? child.end : px;
				frs.add(toFactorizeResult(cs, pos0, pos, child));
			}
			return FactorizeResult.merge(ast.entity, frs);
		} else {
			var pre = Chars.of(cs, p0, ast.start);
			var mid = Chars.of(cs, ast.start, ast.end);
			var post = Chars.of(cs, ast.end, px);
			return new FactorizeResult(pre, new FTerminal(mid), post);
		}
	}

	public Ast check(String entity, String s) {
		return engine.check(entity, s);
	}

	public Ast parse(String entity, String s) {
		return engine.parse(entity, s);
	}

}
