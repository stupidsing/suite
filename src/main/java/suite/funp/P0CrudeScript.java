package suite.funp;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.io.FileReader;
import java.util.ArrayList;

import primal.statics.Fail;
import suite.Suite;
import suite.adt.pair.Pair;
import suite.ebnf.Ebnf;
import suite.ebnf.Ebnf.Ast;
import suite.funp.Funp_.Funp;
import suite.funp.P0.Fdt;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpArray;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDoAssignRef;
import suite.funp.P0.FunpField;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpIndex;
import suite.funp.P0.FunpLambda;
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
import suite.node.tree.TreeTuple;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.streamlet.Read;
import suite.util.To;

public class P0CrudeScript {

	public Funp parse(String in) { // "{ return 1 + 2 * 3; }"
		var ebnf = ex(() -> new Ebnf(new FileReader("src/main/ebnf/crude-script.ebnf")));
		var ast = ebnf.parse("crude-script", in);

		var node = new Object() {
			private Node node(Ast ast) {
				var entity = ast.entity;
				var children = ast.children;
				var atom = Atom.of(entity);
				Node node;
				if (entity.startsWith("<"))
					node = new Str(in.substring(ast.start, ast.end));
				else
					node = Read.from(children).reverse().<Node> fold(Atom.NIL, (n, c) -> Tree.ofAnd(node(c), n));
				return TreeTuple.of(atom, node);
			}
		}.node(ast);

		return new Object() {
			private Funp crudeScript(Node node) {
				return new SwitchNode<Funp>(node //
				).match("crude-script (.0,)", a -> {
					return stmt(a);
				}).nonNullResult();
			}

			private Funp stmt(Node node) {
				return new SwitchNode<Funp>(node //
				).match("statement (expression (.0,),)", a -> {
					return expr(a);
				}).match("statement (statement-return (.0,),)", a -> {
					return expr(a);
				}).match("statement1 (.0,)", a -> {
					return stmt(a);
				}).match("statement-block (statement-let (bind (<IDENTIFIER> .0,), .1,), .2)", (a, b, c) -> {
					return FunpDefine.of(Str.str(a), expr(b), stmt(c), Fdt.L_MONO);
				}).match("statement-block (.0,)", a -> {
					return stmt(a);
				}).match("statement-for (.0, .1, .2, .3,)", (a, b, c, d) -> {
					return expr(a);
				}).match("statement-if (.0, .1, .2,)", (a, b, c) -> {
					return FunpIf.of(expr(a), stmt(b), stmt(c));
				}).match("statement-while (.0, .1,)", (a, b) -> {
					return expr(b);
				}).nonNullResult();
			}

			private Funp expr(Node node) {
				return new SwitchNode<Funp>(node //
				).match("<IDENTIFIER> .0", s -> {
					return FunpVariable.of(Str.str(s));
				}).match("<INTEGER_LITERAL> .0", s -> {
					return FunpNumber.ofNumber(Integer.valueOf(Str.str(s)));
				}).match("<STRING_LITERAL> .0", s -> {
					return fail();
				}).match("constant (.0,)", a -> {
					return expr(a);
				}).match("expression (.0,)", a -> {
					return expr(a);
				}).match("expression-add (.0, .1)", (a, b) -> {
					return Tree.read(b).fold(expr(a), (f, c) -> FunpTree.of(TermOp.PLUS__, f, expr(c)));
				}).match("expression-and (.0,)", a -> {
					return expr(a);
				}).match("expression-as (.0,)", a -> {
					return expr(a);
				}).match("expression-assign (.0, .1)", (a, b) -> {
					return expr(a);
				}).matchArray("expression-array .0", m -> {
					return FunpArray.of(Read.from(m).map(this::expr).toList());
				}).match("expression-bool-and (.0, .1)", (a, b) -> {
					return Tree.read(b).fold(expr(a), (f, c) -> FunpTree.of(TermOp.BIGAND, f, expr(c)));
				}).match("expression-bool-not (.0,)", a -> {
					return expr(a);
				}).match("expression-bool-or (.0, .1)", (a, b) -> {
					return Tree.read(b).fold(expr(a), (f, c) -> FunpTree.of(TermOp.BIGOR_, f, expr(c)));
				}).match("expression-compare (.0,)", a -> {
					return expr(a);
				}).match("expression-dict .0", a -> {
					var list = Tree //
							.read(a) //
							.chunk(2) //
							.map(o -> o.toFixie().map((k, v) -> Pair.of(Str.str(k), expr(v)))) //
							.toList();
					return FunpStruct.of(list);
				}).match("expression-div (.0, .1)", (a, b) -> {
					return Tree //
							.read(b) //
							.chunk(2) //
							.fold(expr(a), (f, o) -> o.toFixie().map((op, d) -> {
								return new SwitchNode<Funp>(op //
								).matchArray("'/'", m_ -> {
									return FunpTree.of(TermOp.DIVIDE, f, expr(d));
								}).matchArray("'%'", m_ -> {
									return FunpTree.of(TermOp.MODULO, f, expr(d));
								}).nonNullResult();
							}));
				}).match("expression-invoke (.0, .1)", (a, b) -> {
					return Tree.read(b).fold(expr(a), (f, c) -> FunpApply.of(f, expr(c)));
				}).match("expression-mul (.0, .1)", (a, b) -> {
					return Tree.read(b).fold(expr(a), (f, c) -> FunpTree.of(TermOp.MULT__, f, expr(c)));
				}).match("expression-not (.0,)", a -> {
					return expr(a);
				}).match("expression-lambda (bind (<IDENTIFIER> (.0,),), expression (.1,),)", (a, b) -> {
					return FunpLambda.of(Str.str(a), expr(b), false);
				}).match("expression-lambda (bind (<IDENTIFIER> (.0,),), statement-block (.1),)", (a, b) -> {
					return FunpLambda.of(Str.str(a), stmt(b), false);
				}).match("expression-obj (.0,)", a -> {
					return expr(a);
				}).match("expression-or (.0,)", a -> {
					return expr(a);
				}).match("expression-pp .0", a -> {
					var pat0 = Suite.pattern("op-inc-dec ('++' (),)");
					var pat1 = Suite.pattern("op-inc-dec ('--' (),)");
					var list = To.list(Tree.read(a));
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
							: FunpDoAssignRef.of(ref0, FunpTree.of(TermOp.PLUS__, e0, FunpNumber.ofNumber(pre_)), e0);
					var e2 = post == 0 ? e1 : Fail.<Funp> fail();
					return s == e ? e2 : fail();
				}).match("expression-prop (.0, .1)", (a, b) -> {
					return Tree //
							.read(b) //
							.chunk(2) //
							.fold(expr(a), (f, o) -> o.toFixie().map((k, v) -> {
								return new SwitchNode<Funp>(k //
								).matchArray("'.'", m_ -> {
									return FunpField.of(FunpReference.of(f), Str.str(v));
								}).matchArray("'['", m_ -> {
									return FunpIndex.of(FunpReference.of(f), expr(v));
								}).matchArray("'('", m_ -> {
									return FunpApply.of(expr(v), f);
								}).nonNullResult();
							}));
				}).match("expression-shift (.0,)", a -> {
					return expr(a);
				}).match("expression-sub (.0,)", a -> {
					return expr(a);
				}).match("expression-tuple .0", a -> {
					var list = new ArrayList<Pair<String, Funp>>();
					var i = 0;
					for (var child : Tree.read(a))
						list.add(Pair.of("t" + i++, expr(child)));
					return FunpStruct.of(list);
				}).match("expression-xor (.0,)", a -> {
					return expr(a);
				}).nonNullResult();
			}
		}.crudeScript(node);
	}

}
