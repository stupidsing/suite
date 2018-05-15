package suite.funp;

import java.io.FileReader;
import java.util.ArrayList;

import suite.Suite;
import suite.adt.pair.Pair;
import suite.ebnf.Ebnf;
import suite.ebnf.Ebnf.Ast;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpArray;
import suite.funp.P0.FunpAssignReference;
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
import suite.primitive.IntPrimitives.Obj_Int;
import suite.streamlet.Read;
import suite.util.Fail;
import suite.util.Rethrow;
import suite.util.To;

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
				}).match1("expression (.0,)", a -> {
					return expr(a);
				}).match2("expression-add (.0, .1)", (a, b) -> {
					return Read.from(Tree.iter(b)).fold(expr(a), (f, c) -> FunpTree.of(TermOp.PLUS__, f, expr(c)));
				}).match1("expression-as (.0,)", a -> {
					return expr(a);
				}).match("expression-array .0", m -> {
					return FunpArray.of(Read.from(m).map(this::expr).toList());
				}).match2("expression-bool-and (.0, .1)", (a, b) -> {
					return Read.from(Tree.iter(b)).fold(expr(a), (f, c) -> FunpTree.of(TermOp.BIGAND, f, expr(c)));
				}).match1("expression-bool-not (.0,)", a -> {
					return expr(a);
				}).match2("expression-bool-or (.0, .1)", (a, b) -> {
					return Read.from(Tree.iter(b)).fold(expr(a), (f, c) -> FunpTree.of(TermOp.BIGOR_, f, expr(c)));
				}).match1("expression-dict .0", a -> {
					var list = Read //
							.from(Tree.iter(a)) //
							.chunk(2) //
							.map(o -> o.toFixie().map((k, v) -> Pair.of(str(k), expr(v)))) //
							.toList();
					return FunpStruct.of(list);
				}).match2("expression-div (.0, .1)", (a, b) -> {
					return Read //
							.from(Tree.iter(b)) //
							.chunk(2) //
							.fold(expr(a), (f, o) -> o.toFixie().map((op, d) -> {
								return new SwitchNode<Funp>(op //
								).match("'/'", m_ -> {
									return FunpTree.of(TermOp.DIVIDE, f, expr(d));
								}).match("'%'", m_ -> {
									return FunpTree.of(TermOp.MODULO, f, expr(d));
								}).nonNullResult();
							}));
				}).match2("expression-invoke (.0, .1)", (a, b) -> {
					return Read.from(Tree.iter(b)).fold(expr(a), (f, c) -> FunpApply.of(f, expr(c)));
				}).match2("expression-mul (.0, .1)", (a, b) -> {
					return Read.from(Tree.iter(b)).fold(expr(a), (f, c) -> FunpTree.of(TermOp.MULT__, f, expr(c)));
				}).match1("expression-not (.0,)", a -> {
					return expr(a);
				}).match1("expression-obj (.0,)", a -> {
					return expr(a);
				}).match1("expression-pp .0", a -> {
					var pat0 = Suite.pattern("op-inc-dec ('++' (),)");
					var pat1 = Suite.pattern("op-inc-dec ('--' (),)");
					var list = To.list(Tree.iter(a));
					Obj_Int<Node> f = op -> pat0.match(op) != null ? 1 : pat1.match(op) != null ? -1 : 0;

					int s = 0, e = list.size() - 1, c;
					int pre_ = 0, post = 0;

					while ((c = f.apply(list.get(s))) != 0) {
						pre_ += c;
						s++;
					}

					while ((c = f.apply(list.get(e))) != 0) {
						post += c;
						e--;
					}

					var e0 = expr(list.get(s));
					var ref0 = FunpReference.of(e0);

					var e1 = pre_ == 0 ? e0
							: FunpAssignReference.of(ref0, FunpTree.of(TermOp.PLUS__, e0, FunpNumber.ofNumber(pre_)), e0);
					var e2 = post == 0 ? e1 : Fail.<Funp> t();
					return s == e ? e2 : Fail.t();
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
