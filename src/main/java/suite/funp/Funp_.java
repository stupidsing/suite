package suite.funp;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import suite.Suite;
import suite.adt.pair.Pair;
import suite.assembler.Amd64;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpReg;
import suite.funp.P0.FunpCoerce.Coerce;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpVariable;
import suite.funp.P2.FunpFramePointer;
import suite.inspect.Inspect;
import suite.node.Node;
import suite.node.util.Singleton;
import suite.object.AutoInterface;
import suite.persistent.PerMap;
import suite.primitive.Bytes;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.Read;
import suite.util.RunUtil;

public class Funp_ {

	public static boolean isAmd64 = RunUtil.isLinux64();
	private static Amd64 amd64 = Amd64.me;

	public static int booleanSize = 1;
	public static int integerSize = 4;
	public static int pointerSize = isAmd64 ? 8 : 4;
	public static int pushSize = isAmd64 ? 8 : 4;
	public static OpReg[] integerRegs = amd64.regs(integerSize);
	public static OpReg[] pointerRegs = amd64.regs(pointerSize);
	public static OpReg[] pushRegs = amd64.regs(pushSize);
	public static OpReg _sp = isAmd64 ? amd64.rsp : amd64.esp;
	public static int nRegisters = isAmd64 ? 16 : 8;
	public static Funp framePointer = new FunpFramePointer();

	private static Inspect inspect = Singleton.me.inspect;

	private boolean isOptimize;

	public interface Funp extends AutoInterface<Funp> {
	}

	public static class CompileException extends RuntimeException {
		private static final long serialVersionUID = 1l;
		public final Funp funp;

		public CompileException(Funp funp, String m, Exception ex) {
			super(m, ex);
			this.funp = funp;
		}
	}

	private Funp_(boolean isOptimize) {
		this.isOptimize = isOptimize;
	}

	public static Main main(boolean isOptimize) {
		return new Funp_(isOptimize).new Main();
	}

	public class Main {
		private P0Parse p0 = new P0Parse();
		private P1Inline p1 = new P1Inline();
		private P1ReduceTailCall p1r = new P1ReduceTailCall();
		private P2InferType p2 = new P2InferType();
		private P2GenerateLambda p2g = new P2GenerateLambda();
		private P3Optimize p3 = new P3Optimize();
		private P4GenerateCode p4 = new P4GenerateCode(!isOptimize);

		private Main() {
		}

		public Pair<List<Instruction>, Bytes> compile(int offset, String fp) {
			var node = Suite.parse(fp);
			var n0 = p0.parse(node);
			var n1 = p1r.reduce(n0);
			var n2 = p1.inline(n1, isOptimize ? 3 : 0, 1, 1, 1, 1, 1);
			var n3 = p2.infer(n2);
			var n4 = p3.optimize(n3);
			return p4.compile(offset, n4);
		}

		public int interpret(Node node) {
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
				).applyIf(FunpDefine.class, f -> f.apply((vn, value, expr, type) -> {
					associate(vars, value);
					associate(vars.replace(vn, f), expr);
					return n_;
				})).applyIf(FunpDefineRec.class, f -> f.apply((pairs, expr) -> {
					var vars1 = Read.from(pairs).fold(vars, (vs, pair) -> vs.replace(pair.t0, f));
					for (var pair : pairs)
						associate(vars1, pair.t1);
					associate(vars1, expr);
					return n_;
				})).applyIf(FunpLambda.class, f -> f.apply((vn, expr, isCapture) -> {
					associate(vars.replace(vn, f), expr);
					return n_;
				})).applyIf(FunpVariable.class, f -> f.apply(vn -> {
					defByVariables.put(f, vars.get(vn));
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
		var m1 = n != null ? m0 + "\nfor " + n.getClass().getSimpleName() : m0;
		throw new CompileException(n, m1, null);
	}

}
