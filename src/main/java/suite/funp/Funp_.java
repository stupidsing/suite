package suite.funp;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import suite.Suite;
import suite.adt.Mutable;
import suite.adt.pair.Fixie_.FixieFun0;
import suite.adt.pair.Fixie_.FixieFun1;
import suite.adt.pair.Fixie_.FixieFun2;
import suite.adt.pair.Fixie_.FixieFun3;
import suite.adt.pair.Pair;
import suite.assembler.Amd64.Instruction;
import suite.funp.P1.FunpFramePointer;
import suite.funp.P1GenerateLambda.Int;
import suite.funp.P1GenerateLambda.Rt;
import suite.funp.P1GenerateLambda.Thunk;
import suite.funp.P1GenerateLambda.Value;
import suite.immutable.IMap;
import suite.node.Node;
import suite.os.LogUtil;
import suite.primitive.Bytes;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.Rethrow;
import suite.util.String_;

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
		private P1InferType p1 = new P1InferType();
		private P1GenerateLambda p1g = new P1GenerateLambda();
		private P2Optimize p2 = new P2Optimize();
		private P3GenerateCode p3 = new P3GenerateCode();

		private Main() {
		}

		public Bytes compile(String fp) {
			Node node = Suite.parse(fp);
			Funp f0 = p0.parse(node);
			Funp f1 = p1.infer(f0);
			Funp f2 = p2.optimize(f1);
			List<Instruction> instructions = p3.compile0(f2);
			return p3.compile1(0, instructions, true);
		}

		public int interpret(Node node) {
			Funp f0 = p0.parse(node);
			p1.infer(f0);
			Thunk thunk = p1g.compile(0, IMap.empty(), f0);
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
			Class<? extends Funp> clazz = node.getClass();

			Method m = Read.from(clazz.getMethods()) //
					.filter(method -> String_.equals(method.getName(), "apply")) //
					.uniqueResult();

			Class<?> type = m.getParameters()[0].getType();
			Mutable<Object> mut = Mutable.nil();

			Fun<Object, Object> set = o -> {
				mut.set(o);
				return true;
			};

			Object p;
			if (type == FixieFun0.class)
				p = (FixieFun0<?>) () -> set.apply(List.of());
			else if (type == FixieFun1.class)
				p = (FixieFun1<?, ?>) (o0) -> set.apply(List.of(o0));
			else if (type == FixieFun2.class)
				p = (FixieFun2<?, ?, ?>) (o0, o1) -> set.apply(List.of(o0, o1));
			else if (type == FixieFun3.class)
				p = (FixieFun3<?, ?, ?, ?>) (o0, o1, o2) -> set.apply(List.of(o0, o1, o2));
			else
				throw new RuntimeException();

			Rethrow.ex(() -> m.invoke(node, p));

			sb.append(clazz.getSimpleName());
			sb.append("{");
			for (Object object : (Collection<?>) mut.get()) {
				dump(object);
				sb.append(",");
			}
			sb.append("}");
		}
	}

}
