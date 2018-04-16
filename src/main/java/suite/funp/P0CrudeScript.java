package suite.funp;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Objects;

import suite.adt.pair.Pair;
import suite.ebnf.Ebnf;
import suite.ebnf.Ebnf.Ast;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpArray;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpStruct;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpVariable;
import suite.node.io.TermOp;
import suite.streamlet.Read;
import suite.util.Fail;
import suite.util.Rethrow;

public class P0CrudeScript {

	public Funp parse(String in) { // "{ return 1 + 2 * 3; }"
		var ebnf = Rethrow.ex(() -> new Ebnf(new FileReader("src/main/ebnf/crude-script.ebnf")));
		var ast = ebnf.parse("crude-script", in);

		return new Object() {
			private Funp expr(Ast a) {
				var children = a.children;
				var c0 = 0 < children.size() ? children.get(0) : null;
				var c1 = 1 < children.size() ? children.get(1) : null;

				if (Objects.equals(a.entity, "expression-invoke")) {
					var funp = expr(c0);
					for (var i = 1; i < children.size(); i++)
						funp = FunpApply.of(funp, expr(children.get(i)));
					return funp;
				} else if (Objects.equals(a.entity, "expression-add"))
					return c1 == null ? expr(c0) : FunpTree.of(TermOp.PLUS__, expr(c0), expr(c1));
				else if (Objects.equals(a.entity, "expression-obj"))
					return expr(c0);
				else if (Objects.equals(a.entity, "expression-dict")) {
					var list = new ArrayList<Pair<String, Funp>>();
					var iter = children.iterator();
					while (iter.hasNext()) {
						var k = iter.next();
						var v = iter.next();
						list.add(Pair.of(str(k), expr(v)));
					}
					return FunpStruct.of(list);
				} else if (Objects.equals(a.entity, "expression-array"))
					return FunpArray.of(Read.from(children).map(this::expr).toList());
				else if (Objects.equals(a.entity, "expression-tuple")) {
					var list = new ArrayList<Pair<String, Funp>>();
					var i = 0;
					for (var child : children)
						list.add(Pair.of("t" + i++, expr(child)));
					return FunpStruct.of(list);
				} else if (Objects.equals(a.entity, "constant"))
					return expr(c0);
				else if (Objects.equals(a.entity, "<IDENTIFIER>"))
					return FunpVariable.of(str(a));
				else if (Objects.equals(a.entity, "<INTEGER_LITERAL>"))
					return FunpNumber.ofNumber(Integer.valueOf(str(a)));
				else if (Objects.equals(a.entity, "<STRING_LITERAL>"))
					return Fail.t();
				else
					return Fail.t();
			}

			private String str(Ast a) {
				return in.substring(a.start, a.end);
			}
		}.expr(ast);
	}

}
