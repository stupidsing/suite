package suite.funp; import java.util.List;

import suite.Suite;
import suite.adt.pair.Pair;
import suite.assembler.Amd64.Instruction;
import suite.funp.P2.FunpFramePointer;
import suite.node.Node;
import suite.object.AutoInterface;
import suite.primitive.Bytes;

public class Funp_ {

	public static int booleanSize = 1;
	public static int integerSize = 4;
	public static int pointerSize = 4;
	public static Funp framePointer = new FunpFramePointer();

	private boolean isOptimize;

	public interface Funp extends AutoInterface<Funp> {
	}

	public static class CompileException extends RuntimeException {
		private static final long serialVersionUID = 1l;

		public CompileException(String m) {
			super(m);
		}

		public <T> T rethrow(Object key) {
			return Funp_.fail(getMessage() + "\nin " + key);
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
			var f0 = p0.parse(node);
			var f1 = p1r.reduce(f0);
			var f2 = p1.inline(f1, isOptimize ? 3 : 0, 1, 1, 1, 1);
			var f3 = p2.infer(f2);
			var f4 = p3.optimize(f3);
			var instructions = p4.compile0(f4);
			return Pair.of(instructions, p4.compile1(offset, instructions, true));
		}

		public int interpret(Node node) {
			var f0 = p0.parse(node);
			p2.infer(f0);
			return p2g.eval(f0);
		}
	}

	public static <T> T fail(String m) {
		throw new CompileException(m);
	}

}
