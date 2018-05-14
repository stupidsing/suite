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
				}).match1("expression-dict .0", a -> {
					var list = Read //
							.from(Tree.iter(a)) //
							.chunk(2) //
							.map(o -> o.toFixie().map((k, v) -> Pair.of(str(k), expr(v)))) //
							.toList();
					return FunpStruct.of(list);
				}).match2("expression-invoke (.0, .1)", (a, b) -> {
					return Read.from(Tree.iter(b)).fold(expr(a), (f, c) -> FunpApply.of(f, expr(c)));
				}).match1("expression-obj (.0,)", a -> {
					return expr(a);
				}).match2("expression-prop (.0, .1)", (a, b) -> {
					return Read //
							.from(Tree.iter(b)) //
							.chunk(2) //
							.fold(expr(a), (f, o) -> o.toFixie().map((k, v) -> {
								return new SwitchNode<Funp>(k //
								).match("'.'", m_ -> {
									return FunpField.of(FunpReference.of(f), str(v));
								}).match("'['", m_ -> {
									return FunpIndex.of(FunpReference.of(f), expr(v));
								}).match("'('", m_ -> {
									return FunpApply.of(expr(v), f);
								}).match(Atom.NIL, m_ -> {
									return f;
								}).nonNullResult();
							}));
				}).match1("expression-tuple .0", a -> {
					var list = new ArrayList<Pair<String, Funp>>();
					var i = 0;
					for (var child : Tree.iter(a))
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
