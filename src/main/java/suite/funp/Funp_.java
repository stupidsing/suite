package suite.funp;

import java.util.List;

import suite.Suite;
import suite.adt.pair.Pair;
import suite.assembler.Amd64.Instruction;
import suite.funp.P2.FunpFramePointer;
import suite.node.Node;
import suite.primitive.Bytes;
import suite.util.AutoInterface;

public class Funp_ {

	public static int booleanSize = 1;
	public static int integerSize = 4;
	public static int pointerSize = 4;
	public static Funp framePointer = new FunpFramePointer();

	private boolean isOptimize;

	public interface Funp extends AutoInterface<Funp> {
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
		private P2InferType p2 = new P2InferType();
		private P2GenerateLambda p2g = new P2GenerateLambda();
		private P3Optimize p3 = new P3Optimize();
		private P4GenerateCode p4 = new P4GenerateCode(!isOptimize);

		private Main() {
		}

		public Pair<List<Instruction>, Bytes> compile(int offset, String fp) {
			var node = Suite.parse(fp);
			var f0 = p0.parse(node);
			var f1 = p1.inline(f0, isOptimize ? 3 : 0, 1, 1, 1, 1);
			var f2 = p2.infer(f1);
			var f3 = p3.optimize(f2);
			var instructions = p4.compile0(f3);
			return Pair.of(instructions, p4.compile1(offset, instructions, true));
		}

		public int interpret(Node node) {
			var f0 = p0.parse(node);
			p2.infer(f0);
			return p2g.eval(f0);
		}
	}

}
