package suite.funp;

import java.util.Collection;
import java.util.List;

import suite.Suite;
import suite.adt.pair.Pair;
import suite.assembler.Amd64.Instruction;
import suite.funp.P2.FunpFramePointer;
import suite.funp.P2GenerateLambda.Int;
import suite.funp.P2GenerateLambda.Rt;
import suite.immutable.IMap;
import suite.node.Node;
import suite.os.LogUtil;
import suite.primitive.Bytes;
import suite.util.AutoInterface;
import suite.util.MapObject_;
import suite.util.Switch;

public class Funp_ {

	public static int booleanSize = 1;
	public static int integerSize = 4;
	public static int pointerSize = 4;
	public static FunpFramePointer framePointer = new FunpFramePointer();

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
			var thunk = p2g.compile(0, IMap.empty(), f0);
			var value = thunk.apply(new Rt(null, null));
			return ((Int) value).i;
		}
	}

	public static void dump(Funp node) {
		var sb = new StringBuilder();

		new Object() {
			private void dump(Object object) {
				new Switch<Object>(object //
				).doIf(Funp.class, node -> {
					sb.append(node.getClass().getSimpleName());
					sb.append("{");
					for (var object1 : MapObject_.list(node)) {
						dump(object1);
						sb.append(",");
					}
					sb.append("}");
				}).doIf(Collection.class, collection -> {
					sb.append("[");
					for (var object1 : collection) {
						dump(object1);
						sb.append(",");
					}
					sb.append("]");
				}).doIf(Pair.class, pair -> {
					sb.append("<");
					dump(pair.t0);
					sb.append(", ");
					dump(pair.t1);
					sb.append(">");
				}).doIf(Object.class, o -> {
					sb.append(object != null ? object.toString() : "null");
				}).nonNullResult();
			}
		}.dump(node);

		LogUtil.info(sb.toString());
	}

}
