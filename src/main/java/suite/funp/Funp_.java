package suite.funp;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import primal.MoreVerbs.Read;
import primal.adt.Pair;
import primal.fp.Funs.Source;
import primal.persistent.PerMap;
import primal.primitive.adt.Bytes;
import primal.primitive.adt.pair.IntObjPair;
import suite.BindArrayUtil;
import suite.BindArrayUtil.Pattern;
import suite.assembler.Amd64;
import suite.assembler.Amd64.Instruction;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpDoAssignRef;
import suite.funp.P0.FunpField;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpPredefine;
import suite.funp.P0.FunpRemark;
import suite.funp.P0.FunpTag;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpTree2;
import suite.funp.P0.FunpVariable;
import suite.funp.P0.FunpVariableNew;
import suite.funp.p0.P0Parse;
import suite.funp.p1.P10Check;
import suite.funp.p1.P11ReduceTailCall;
import suite.funp.p1.P12Inline;
import suite.funp.p2.P20ExtractPredefine;
import suite.funp.p2.P21CaptureLambda;
import suite.funp.p2.P22InferType;
import suite.funp.p2.P2GenerateLambda;
import suite.funp.p3.P3Optimize;
import suite.funp.p4.P4GenerateCode;
import suite.inspect.Inspect;
import suite.node.Node;
import suite.node.io.SwitchNode;
import suite.node.parser.IterativeParser;
import suite.node.util.Singleton;
import suite.object.CastDefaults;
import suite.object.MetadataDefaults;
import suite.object.SwitchDefaults;
import suite.util.Switch;

public class Funp_ extends FunpCfg {

	private static Inspect inspect = Singleton.me.inspect;
	private static IterativeParser parser = new IterativeParser(FunpOp.values, FunpOp.TUPLE_);

	private static BindArrayUtil bindArrayUtil = new BindArrayUtil("€", parser);

	public boolean isOptimize;

	public interface Funp extends CastDefaults<Funp>, MetadataDefaults<FunpMetadata>, SwitchDefaults<Funp> {
	}

	public static class FunpMetadata {
	}

	public static class CompileException extends RuntimeException {
		private static final long serialVersionUID = 1l;
		public final Funp funp;

		public CompileException(Funp funp, String m, Exception ex) {
			super((ex != null ? ex.getMessage() + "\n" : "") + m, ex);
			this.funp = funp;
		}
	}

	public static Main main(boolean isLongMode, boolean isOptimize) {
		return new Funp_(isLongMode, isOptimize).new Main();
	}

	public Funp_(boolean isLongMode, boolean isOptimize) {
		super(Amd64.me, isLongMode);
		this.isOptimize = isOptimize;
	}

	public class Main {
		private Main() {
		}

		public Pair<List<Instruction>, Bytes> compile(int offset, String fp) {
			var f = Funp_.this;
			var p0 = new P0Parse(f);
			var p10 = new P10Check(f);
			var p11 = new P11ReduceTailCall(f);
			var p12 = new P12Inline(f);
			var p20 = new P20ExtractPredefine();
			var p21 = new P21CaptureLambda();
			var p22 = new P22InferType(f);
			var p3 = new P3Optimize(f);
			var p4 = new P4GenerateCode(f);

			var node = parse(fp);
			var n0 = p0.parse(node);
			var n1 = p10.check(n0);
			var n2 = p11.reduce(n1);
			var n3 = p12.inline(n2, isOptimize ? 3 : 0);
			var n4 = p20.extractPredefine(n3);
			var n5 = p21.captureLambdas(n4);
			var n6 = p22.infer(n5);
			var n7 = p3.optimize(n6);
			return p4.compile(offset, n7);
		}

		public int interpret(Node node) {
			var f = Funp_.this;
			var p0 = new P0Parse(f);
			var p2 = new P22InferType(f);
			var p2g = new P2GenerateLambda(f);

			var f0 = p0.parse(node);
			p2.infer(f0);
			return p2g.eval(f0);
		}
	}

	public static Map<FunpVariable, IntObjPair<Funp>> associateDefinitions(Funp node) {
		var defByVariables = new IdentityHashMap<FunpVariable, IntObjPair<Funp>>();

		new Object() {
			private Funp associate(PerMap<String, IntObjPair<Funp>> vars, Funp node_) {
				return inspect.rewrite(node_, Funp.class, n_ -> n_.sw( //
				).applyIf(FunpDefine.class, f -> f.apply((vn, value, expr, fdt) -> {
					associate(vars, value);
					associate(vars.replace(vn, IntObjPair.of(0, f)), expr);
					return n_;
				})).applyIf(FunpDefineRec.class, f -> f.apply((pairs, expr, fdt) -> {
					var vars1 = vars;
					var i = 0;
					for (var pair : pairs)
						vars1 = vars1.replace(pair.k, IntObjPair.of(i++, f));
					for (var pair : pairs)
						associate(vars1, pair.v);
					associate(vars1, expr);
					return n_;
				})).applyIf(FunpLambda.class, f -> f.apply((vn, expr, fct) -> {
					associate(vars.replace(vn, IntObjPair.of(0, f)), expr);
					return n_;
				})).applyIf(FunpVariable.class, f -> f.apply(vn -> {
					defByVariables.put(f, vars.getOrFail(vn));
					return n_;
				})).result());
			}
		}.associate(PerMap.empty(), node);

		return defByVariables;
	}

	public static <T> T fail(Funp n, String m0) {
		var m1 = n != null ? m0 + "\nin construct " + describe(n) : m0;
		throw new CompileException(n, m1, null);
	}

	public static Node parse(String in) {
		return parser.parse(in);
	}

	public static Pattern pattern(String pattern) {
		return bindArrayUtil.pattern(pattern);
	}

	public static <T> T rethrow(String in, Source<T> source) {
		try {
			return source.g();
		} catch (CompileException ex) {
			throw new CompileException(ex.funp, "in " + in, ex);
		} catch (Exception ex) {
			throw new RuntimeException(ex.getMessage() + "\nin " + in, ex);
		}
	}

	public static Node substitute(String pattern, Node... nodes) {
		return bindArrayUtil.pattern(pattern).subst(nodes);
	}

	public static <T> SwitchNode<T> switchNode(Node in) {
		return new SwitchNode<>(in, bindArrayUtil::pattern);
	}

	private static String describe(Funp n) {
		var c = n.getClass().getSimpleName();

		return new Switch<String>(n) //
				.applyIf(FunpDefine.class, f -> c + " (" + f.vn + ")") //
				.applyIf(FunpDefineRec.class, f -> c + " (" + Read.from2(f.pairs).keys().toJoinedString(", ") + ")") //
				.applyIf(FunpDoAssignRef.class, f -> c + " (" + describe(f.reference) + ")") //
				.applyIf(FunpField.class, f -> c + " (" + f.field + ")") //
				.applyIf(FunpLambda.class, f -> c + " (" + f.vn + ")") //
				.applyIf(FunpPredefine.class, f -> c + " (" + f.vn + ")") //
				.applyIf(FunpRemark.class, f -> c + " (" + f.remark + ")") //
				.applyIf(FunpTag.class, f -> c + " (" + f.tag + ")") //
				.applyIf(FunpTree.class, f -> c + " (" + f.operator.name_().trim() + ")") //
				.applyIf(FunpTree2.class, f -> c + " (" + f.operator.name + ")") //
				.applyIf(FunpVariable.class, f -> c + " (" + f.vn + ")") //
				.applyIf(FunpVariableNew.class, f -> c + " (" + f.vn + ")") //
				.applyIf(Funp.class, f -> c) //
				.nonNullResult();
	}

}
