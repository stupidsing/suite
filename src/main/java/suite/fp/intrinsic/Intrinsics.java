package suite.fp.intrinsic;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.immutable.IPointer;
import suite.instructionexecutor.IndexedSourceReader;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.primitive.Chars;
import suite.util.FunUtil.Fun;
import suite.util.LogUtil;
import suite.util.Util;

public class Intrinsics {

	private static int bufferSize = 4096;

	public interface Intrinsic {
		public Node invoke(IntrinsicCallback callback, List<Node> inputs);
	}

	public interface IntrinsicCallback {
		public Node yawn(Node node);

		public Node enclose(Intrinsic intrinsic, Node node);
	}

	public static Map<String, Intrinsic> intrinsics = new HashMap<>();

	// Forces suspended node evaluation
	public static Intrinsic id_ = (callback, inputs) -> inputs.get(0).finalNode();

	public static <T> Intrinsic drain(List<T> list, Fun<T, Node> fun) {
		Intrinsic drains[] = new Intrinsic[1];
		drains[0] = new Intrinsic() {
			public Node invoke(IntrinsicCallback callback, List<Node> inputs) {
				List<T> list = Data.get(inputs.get(0));

				if (!list.isEmpty()) {
					Node left = callback.enclose(Intrinsics.id_, fun.apply(list.get(0)));
					Node right = callback.enclose(drains[0], new Data<>(Util.right(list, 1)));
					return Tree.of(TermOp.OR____, left, right);
				} else
					return Atom.NIL;
			}
		};
		return (callback, inputs) -> callback.enclose(drains[0], new Data<>(list));
	}

	public static Node enclose(IntrinsicCallback callback, Node node) {
		return callback.enclose(id_, node);
	}

	public static IPointer<Chars> read(Reader reader) {
		return new IndexedSourceReader<Chars>(() -> {
			try {
				char buffer[] = new char[bufferSize];
				int nCharsRead = reader.read(buffer);
				if (nCharsRead >= 0)
					return Chars.of(buffer, 0, nCharsRead);
				else {
					Util.closeQuietly(reader);
					return null;
				}
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}).pointer();
	}

	static {
		for (Class<?> clazz : Arrays.asList( //
				ArrayIntrinsics.class //
				, BasicIntrinsics.class //
				, CharsIntrinsics.class //
				, MonadIntrinsics.class //
				, SeqIntrinsics.class)) {
			Object instance;

			try {
				instance = clazz.newInstance();
			} catch (ReflectiveOperationException ex) {
				throw new RuntimeException(ex);
			}

			for (Field field : clazz.getFields())
				if (Intrinsic.class.isAssignableFrom(field.getType()))
					try {
						intrinsics.put(clazz.getSimpleName() + "." + field.getName(), (Intrinsic) field.get(instance));
					} catch (Exception ex) {
						LogUtil.error(ex);
					}
		}
	}

}
