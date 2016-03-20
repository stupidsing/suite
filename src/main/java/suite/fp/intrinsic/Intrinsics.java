package suite.fp.intrinsic;

import java.io.Reader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import suite.immutable.IPointer;
import suite.instructionexecutor.thunk.IndexedReader;
import suite.instructionexecutor.thunk.IndexedSourceReader;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.os.LogUtil;
import suite.primitive.Chars;
import suite.util.Rethrow;
import suite.util.Util;

public class Intrinsics {

	private static int bufferSize = 4096;

	public interface Intrinsic {
		public Node invoke(IntrinsicCallback callback, List<Node> inputs);
	}

	public interface IntrinsicCallback {

		/**
		 * Encloses an intrinsic function call with given parameter into a lazy
		 * result node. Becomes immediate evaluation in the eager
		 * implementation.
		 */
		public Node enclose(Intrinsic intrinsic, Node node);

		/**
		 * Realizes a possibly-lazy node into its bottom value. Returns the
		 * input for the eager implementation.
		 */
		public Node yawn(Node node);
	}

	public static Map<String, Intrinsic> intrinsics = new HashMap<>();

	public static IntrinsicCallback EagerIntrinsicCallback = new IntrinsicCallback() {
		public Node enclose(Intrinsic intrinsic, Node node) {
			return intrinsic.invoke(this, Arrays.asList(node));
		}

		public Node yawn(Node node) {
			return node;
		}
	};

	// Forces suspended node evaluation
	public static Intrinsic id_ = (callback, inputs) -> inputs.get(0);

	public static <T> Node drain(IntrinsicCallback callback, IntFunction<Node> read, int size) {
		return drain(callback, IndexedReader.of(read, size));
	}

	public static Node drain(IntrinsicCallback callback, IPointer<Node> pointer) {
		Intrinsic drains[] = new Intrinsic[1];
		drains[0] = (callback1, inputs) -> {
			IPointer<Node> pointer1 = Data.get(inputs.get(0));
			Node head;

			if ((head = pointer1.head()) != null) {
				Node left = callback1.enclose(Intrinsics.id_, head);
				Node right = callback1.enclose(drains[0], new Data<>(pointer1.tail()));
				return Tree.of(TermOp.OR____, left, right);
			} else
				return Atom.NIL;
		};
		return callback.yawn(callback.enclose(drains[0], new Data<>(pointer)));
	}

	public static Node enclose(IntrinsicCallback callback, Node node) {
		return callback.enclose(id_, node);
	}

	public static IPointer<Chars> read(Reader reader) {
		return IndexedSourceReader.of(() -> Rethrow.ioException(() -> {
			char buffer[] = new char[bufferSize];
			int nCharsRead = reader.read(buffer);
			if (0 <= nCharsRead)
				return Chars.of(buffer, 0, nCharsRead);
			else {
				Util.closeQuietly(reader);
				return null;
			}
		}));
	}

	static {
		for (Class<?> clazz : Arrays.asList( //
				ArrayIntrinsics.class //
				, BasicIntrinsics.class //
				, CharsIntrinsics.class //
				, MonadIntrinsics.class //
				, SeqIntrinsics.class //
				, SuiteIntrinsics.class)) {
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
