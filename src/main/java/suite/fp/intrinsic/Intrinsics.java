package suite.fp.intrinsic;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import suite.cfg.Defaults;
import suite.immutable.IPointer;
import suite.instructionexecutor.thunk.IndexedReader;
import suite.instructionexecutor.thunk.IndexedSourceReader;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Node;
import suite.node.tree.TreeOr;
import suite.primitive.Chars;
import suite.util.Object_;
import suite.util.Rethrow;

public class Intrinsics {

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

	public static IntrinsicCallback eagerIntrinsicCallback = new IntrinsicCallback() {
		public Node enclose(Intrinsic intrinsic, Node node) {
			return intrinsic.invoke(this, List.of(node));
		}

		public Node yawn(Node node) {
			return node;
		}
	};

	// forces suspended node evaluation
	public static Intrinsic id_ = (callback, inputs) -> inputs.get(0);

	public static <T> Node drain(IntrinsicCallback callback, IntFunction<Node> read, int size) {
		return drain(callback, IndexedReader.of(read, size));
	}

	public static Node drain(IntrinsicCallback callback, IPointer<Node> pointer) {
		var drain = new Intrinsic() {
			public Node invoke(IntrinsicCallback callback1, List<Node> inputs) {
				IPointer<Node> pointer1 = Data.get(inputs.get(0));
				Node head;

				if ((head = pointer1.head()) != null) {
					var left = callback1.enclose(Intrinsics.id_, head);
					var right = callback1.enclose(this::invoke, new Data<>(pointer1.tail()));
					return TreeOr.of(left, right);
				} else
					return Atom.NIL;
			}
		};

		return callback.yawn(callback.enclose(drain, new Data<>(pointer)));
	}

	public static Node enclose(IntrinsicCallback callback, Node node) {
		return callback.enclose(id_, node);
	}

	public static IPointer<Chars> read(Reader reader) {
		return IndexedSourceReader.of(() -> Rethrow.ex(() -> {
			var buffer = new char[Defaults.bufferSize];
			var nCharsRead = reader.read(buffer);
			if (0 <= nCharsRead)
				return Chars.of(buffer, 0, nCharsRead);
			else {
				Object_.closeQuietly(reader);
				return null;
			}
		}));
	}

	static {
		for (var clazz : List.of( //
				ArrayIntrinsics.class //
				, BasicIntrinsics.class //
				, CharsIntrinsics.class //
				, MonadIntrinsics.class //
				, SeqIntrinsics.class //
				, SuiteIntrinsics.class)) {
			var instance = Object_.new_(clazz);

			for (var field : clazz.getFields())
				if (Intrinsic.class.isAssignableFrom(field.getType())) {
					var name = clazz.getSimpleName() + "." + field.getName();
					Rethrow.ex(() -> intrinsics.put(name, (Intrinsic) field.get(instance)));
				}
		}
	}

}
