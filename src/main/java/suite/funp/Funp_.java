package suite.funp;

import java.util.Collection;
import java.util.List;

import suite.Suite;
import suite.adt.pair.Pair;
import suite.assembler.Amd64.Instruction;
import suite.funp.P2.FunpFramePointer;
import suite.funp.P2GenerateLambda.Int;
import suite.funp.P2GenerateLambda.Rt;
import suite.funp.P2GenerateLambda.Thunk;
import suite.funp.P2GenerateLambda.Value;
import suite.immutable.IMap;
import suite.node.Node;
import suite.os.LogUtil;
import suite.primitive.Bytes;
import suite.util.MapObject_;

public class Funp_ {

	public static Funp_ me = new Funp_();
	public static int booleanSize = 1;
	public static int integerSize = 4;
	public static int pointerSize = 4;

	public static FunpFramePointer framePointer = new FunpFramePointer();

	public interface Funp {
	}

	public static Main main() {
		return new Funp_().new Main();
	}

	public class Main {
		private P0Parse p0 = new P0Parse();
		private P1Inline p1 = new P1Inline();
		private P2InferType p2 = new P2InferType();
		private P2GenerateLambda p2g = new P2GenerateLambda();
		private P3Optimize p3 = new P3Optimize();
		private P4GenerateCode p4 = new P4GenerateCode();

		private Main() {
		}

		public Bytes compile(int offset, String fp) {
			Node node = Suite.parse(fp);
			Funp f0 = p0.parse(node);
			Funp f1 = p1.inline(f0);
			Funp f2 = p2.infer(f1);
			Funp f3 = p3.optimize(f2);
			List<Instruction> instructions = p4.compile0(f3);
			return p4.compile1(offset, instructions, true);
		}

		public int interpret(Node node) {
			Funp f0 = p0.parse(node);
			p2.infer(f0);
			Thunk thunk = p2g.compile(0, IMap.empty(), f0);
			Value value = thunk.apply(new Rt(null, null));
			return ((Int) value).i;
		}
	}

	public static void dump(Funp node) {
		Dump dump = new Dump();
		dump.dump(node);
		LogUtil.info(dump.sb.toString());
	}

	private static class Dump {
		private StringBuilder sb = new StringBuilder();

		private void dump(Object object) {
			if (object instanceof Funp)
				dump_((Funp) object);
			else if (object instanceof Collection<?>) {
				sb.append("[");
				for (Object object1 : (Collection<?>) object) {
					dump(object1);
					sb.append(",");
				}
				sb.append("]");
			} else if (object instanceof Pair<?, ?>) {
				Pair<?, ?> pair = (Pair<?, ?>) object;
				sb.append("<");
				dump(pair.t0);
				sb.append(", ");
				dump(pair.t1);
				sb.append(">");
			} else
				sb.append(object != null ? object.toString() : "null");
		}

		private void dump_(Funp node) {
			List<?> list = MapObject_.list(node);

			sb.append(node.getClass().getSimpleName());
			sb.append("{");
			for (Object object : list) {
				dump(object);
				sb.append(",");
			}
			sb.append("}");
		}
	}

}
