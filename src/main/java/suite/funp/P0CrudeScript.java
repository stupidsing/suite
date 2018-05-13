package suite.funp;

import java.io.FileReader;
import java.util.ArrayList;

import suite.adt.pair.Pair;
import suite.ebnf.Ebnf;
import suite.ebnf.Ebnf.Ast;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpArray;
import suite.funp.P0.FunpField;
import suite.funp.P0.FunpIndex;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpStruct;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpVariable;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.SwitchNode;
import suite.node.io.TermOp;
import suite.streamlet.Read;
import suite.util.Fail;
import suite.util.Rethrow;

public class P0CrudeScript {

	public Funp parse(String in) { // "{ return 1 + 2 * 3; }"
		var ebnf = Rethrow.ex(() -> new Ebnf(new FileReader("src/main/ebnf/crude-script.ebnf")));
		var ast = ebnf.parse("crude-script", in);

		var node = new Object() {
			private Node node(Ast ast) {
				var atom = Atom.of(ast.entity);
				var children = ast.children;
				if (ast.entity.startsWith("<"))
					return Tree.of(TermOp.TUPLE_, atom, new Str(in.substring(ast.start, ast.end)));
				else {
					Node node = Atom.NIL;
					for (var i = children.size() - 1; 0 <= i; i--)
						node = Tree.of(TermOp.AND___, node(children.get(i)), node);
					return Tree.of(TermOp.TUPLE_, atom, node);
				}
			}
		}.node(ast);

		return new Object() {
			private Funp expr(Node node) {
				return new SwitchNode<Funp>(node //
				).match1("constant (.0,)", a -> {
					return expr(a);
				}).match1("expression-add (.0,)", a -> {
					return expr(a);
				}).match2("expression-add (.0, .1,)", (a, b) -> {
					return FunpTree.of(TermOp.PLUS__, expr(a), expr(b));
				}).match("expression-array .0", m -> {
					return FunpArray.of(Read.from(m).map(this::expr).toList());
				}).match("expression-dict .0", m -> {
					var list = new ArrayList<Pair<String, Funp>>();
					for (var i = 0; i < m.length; i += 2)
						list.add(Pair.of(str(m[i]), expr(m[i + 1])));
					return FunpStruct.of(list);
				}).match("expression-invoke .0", m -> {
					var funp = expr(m[0]);
					for (var i = 1; i < m.length; i++)
						funp = FunpApply.of(funp, expr(m[i]));
					return funp;
				}).match1("expression-obj (.0,)", a -> {
					return expr(a);
				}).match("expression-prop (.0,)", m -> {
					var funp = expr(m[0]);
					for (var i = 1; i < m.length; i += 2) {
						var funp_ = funp;
						var m1 = m[i + 1];
						funp = new SwitchNode<Funp>(m[i] //
						).match("'.'", m_ -> {
							return FunpField.of(FunpReference.of(funp_), str(m1));
						}).match("'['", m_ -> {
							return FunpIndex.of(FunpReference.of(funp_), expr(m1));
						}).match("'('", m_ -> {
							return FunpApply.of(expr(m1), funp_);
						}).match(Atom.NIL, m_ -> {
							return funp_;
						}).nonNullResult();
					}
					return funp;
				}).match("expression-tuple .0", m -> {
					var list = new ArrayList<Pair<String, Funp>>();
					var i = 0;
					for (var child : m)
						list.add(Pair.of("t" + i++, expr(child)));
					return FunpStruct.of(list);
				}).match1("<IDENTIFIER> .0", s -> {
					return FunpVariable.of(str(s));
				}).match1("<INTEGER_LITERAL> .0", s -> {
					return FunpNumber.ofNumber(Integer.valueOf(str(s)));
				}).match1("<STRING_LITERAL> .0", s -> {
					return Fail.t();
				}).nonNullResult();
			}

			private String str(Node node) {
				return ((Str) node).value;
			}
		}.expr(node);
	}

}
