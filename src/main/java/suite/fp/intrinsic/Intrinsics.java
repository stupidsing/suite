package suite.fp.intrinsic;

import static primal.statics.Rethrow.ex;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import primal.Nouns.Buffer;
import primal.Verbs.Close;
import primal.Verbs.New;
import primal.primitive.adt.Chars;
import suite.instructionexecutor.thunk.IndexedReader;
import suite.instructionexecutor.thunk.IndexedSourceReader;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Node;
import suite.node.Tree;
import suite.persistent.PerPointer;

public class Intrinsics {

	public interface Intrinsic {
		public Node invoke(IntrinsicCallback callback, List<Node> inputs);
	}

	public interface IntrinsicCallback {

		/**
		 * Encloses an intrinsic function call with given parameter into a lazy result
		 * node. Becomes immediate evaluation in the eager implementation.
		 */
		public Node enclose(Intrinsic intrinsic, Node node);

		/**
		 * Realizes a possibly-lazy node into its bottom value. Returns the input for
		 * the eager implementation.
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

	public static Node drain(IntrinsicCallback callback, PerPointer<Node> pointer) {
		var drain = new Intrinsic() {
			public Node invoke(IntrinsicCallback callback1, List<Node> inputs) {
				PerPointer<Node> pointer1 = Data.get(inputs.get(0));
				Node head;

				if ((head = pointer1.head()) != null) {
					var left = callback1.enclose(Intrinsics.id_, head);
					var right = callback1.enclose(this::invoke, new Data<>(pointer1.tail()));
					return Tree.ofOr(left, right);
				} else
					return Atom.NIL;
			}
		};

		return callback.yawn(callback.enclose(drain, new Data<>(pointer)));
	}

	public static Node enclose(IntrinsicCallback callback, Node node) {
		return callback.enclose(id_, node);
	}

	public static PerPointer<Chars> read(Reader reader) {
		return IndexedSourceReader.of(() -> ex(() -> {
			var buffer = new char[Buffer.size];
			var nCharsRead = reader.read(buffer);
			if (0 <= nCharsRead)
				return Chars.of(buffer, 0, nCharsRead);
			else {
				Close.quietly(reader);
				return null;
			}
		}));
	}

	static {
		for (var clazz : List.of(
				ArrayIntrinsics.class
				, BasicIntrinsics.class
				, CharsIntrinsics.class
				, MonadIntrinsics.class
				, SeqIntrinsics.class
				, SuiteIntrinsics.class)) {
			var instance = New.clazz(clazz);

			for (var field : clazz.getFields())
				if (Intrinsic.class.isAssignableFrom(field.getType())) {
					var name = clazz.getSimpleName() + "." + field.getName();
					ex(() -> intrinsics.put(name, (Intrinsic) field.get(instance)));
				}
		}
	}

}
