package suite.funp;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import primal.MoreVerbs.Read;
import primal.adt.Pair;
import primal.fp.Funs.Source;
import primal.persistent.PerMap;
import primal.primitive.adt.Bytes;
import suite.Suite;
import suite.assembler.Amd64;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64Mode;
import suite.funp.P0.Coerce;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpVariable;
import suite.funp.P2.FunpFramePointer;
import suite.funp.p0.P0Parse;
import suite.funp.p1.P10Inline;
import suite.funp.p1.P11ReduceTailCall;
import suite.funp.p2.P2InferType;
import suite.funp.p3.P3Optimize;
import suite.funp.p4.P4GenerateCode;
import suite.inspect.Inspect;
import suite.node.Node;
import suite.node.util.Singleton;
import suite.object.CastDefaults;
import suite.util.RunUtil;

public class Funp_ {

	private static Amd64 amd64 = Amd64.me;
	private static Inspect inspect = Singleton.me.inspect;

	public static boolean isAmd64 = RunUtil.isLinux64();
	public static Amd64Mode mode = isAmd64 ? Amd64Mode.LONG64 : Amd64Mode.PROT32;
	public static int booleanSize = 1;
	public static int integerSize = mode.opSize;
	public static int pointerSize = mode.addrSize;
	public static int pushSize = mode.pushSize;
	public static OpReg[] integerRegs = amd64.regs(integerSize);
	public static OpReg[] pointerRegs = amd64.regs(pointerSize);
	public static OpReg[] pushRegs = amd64.regs(pushSize);
	public static OpReg _bp = pointerRegs[amd64.bpReg];
	public static OpReg _sp = pointerRegs[amd64.spReg];
	public static int nRegisters = mode.nRegisters;
	public static Funp framePointer = new FunpFramePointer();

	public boolean isOptimize;

	public interface Funp extends CastDefaults<Funp> {
	}

	public static class CompileException extends RuntimeException {
		private static final long serialVersionUID = 1l;
		public final Funp funp;

		public CompileException(Funp funp, String m, Exception ex) {
			super(m, ex);
			this.funp = funp;
		}
	}

	public static Main main(boolean isOptimize) {
		return new Funp_(isOptimize).new Main();
	}

	public Funp_(boolean isOptimize) {
		this.isOptimize = isOptimize;
	}

	public class Main {
		private Main() {
		}

		public Pair<List<Instruction>, Bytes> compile(int offset, String fp) {
			var f = Funp_.this;
			var p0 = new P0Parse(f);
			var p1 = new P10Inline(f);
			var p1r = new P11ReduceTailCall(f);
			var p2 = new P2InferType(f);
			var p3 = new P3Optimize(f);
			var p4 = new P4GenerateCode(f);

			var node = Suite.parse(fp);
			var n0 = p0.parse(node);
			var n1 = p1r.reduce(n0);
			var n2 = p1.inline(n1, isOptimize ? 3 : 0, 1, 1, 1, 1, 1);
			var n3 = p2.infer(n2);
			var n4 = p3.optimize(n3);
			return p4.compile(offset, n4);
		}

		public int interpret(Node node) {
			var f = Funp_.this;
			var p0 = new P0Parse(f);
			var p2 = new P2InferType(f);
			var p2g = new P2GenerateLambda(f);

			var f0 = p0.parse(node);
			p2.infer(f0);
			return p2g.eval(f0);
		}
	}

	public static Map<FunpVariable, Funp> associateDefinitions(Funp node) {
		var defByVariables = new IdentityHashMap<FunpVariable, Funp>();

		new Object() {
			private Funp associate(PerMap<String, Funp> vars, Funp node_) {
				return inspect.rewrite(node_, Funp.class, n_ -> n_.sw( //
				).applyIf(FunpDefine.class, f -> f.apply((vn, value, expr, fdt) -> {
					associate(vars, value);
					associate(vars.replace(vn, f), expr);
					return n_;
				})).applyIf(FunpDefineRec.class, f -> f.apply((pairs, expr, fdt) -> {
					var vars1 = Read.from(pairs).fold(vars, (vs, pair) -> vs.replace(pair.k, f));
					for (var pair : pairs)
						associate(vars1, pair.v);
					associate(vars1, expr);
					return n_;
				})).applyIf(FunpLambda.class, f -> f.apply((vn, expr, fct) -> {
					associate(vars.replace(vn, f), expr);
					return n_;
				})).applyIf(FunpVariable.class, f -> f.apply(vn -> {
					defByVariables.put(f, vars.getOrFail(vn));
					return n_;
				})).result());
			}
		}.associate(PerMap.empty(), node);

		return defByVariables;
	}

	public static int getCoerceSize(Coerce coerce) {
		if (coerce == Coerce.BYTE)
			return 1;
		else if (coerce == Coerce.NUMBER)
			return integerSize;
		else if (coerce == Coerce.NUMBERP || coerce == Coerce.POINTER)
			return pointerSize;
		else
			return fail(null, "");
	}

	public static boolean isSigned(Coerce coerce) {
		return coerce == Coerce.BYTE || coerce == Coerce.NUMBER || coerce == Coerce.NUMBERP;
	}

	public static boolean isSizeOk(long scale) {
		return scale == 1 || scale == 2 || scale == 4 || Funp_.isAmd64 && scale == 8;
	}

	public static boolean is1248(long scale) {
		return scale == 1 || scale == 2 || scale == 4 || scale == 8;
	}

	public static <T> T rethrow(String in, Source<T> source) {
		try {
			return source.g();
		} catch (CompileException ex) {
			throw new CompileException(ex.funp, ex.getMessage() + "\nin " + in, ex);
		} catch (Exception ex) {
			throw new RuntimeException("in " + in, ex);
		}
	}

	public static <T> T fail(Funp n, String m0) {
		var m1 = n != null ? m0 + "\nfor construct '" + n.getClass().getSimpleName() + "'" : m0;
		throw new CompileException(n, m1, null);
	}

}
