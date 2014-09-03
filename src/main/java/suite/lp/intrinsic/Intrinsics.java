package suite.lp.intrinsic;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.node.Node;
import suite.util.LogUtil;

public class Intrinsics {

	public interface Intrinsic {
		public Node invoke(IntrinsicBridge bridge, List<Node> inputs);
	}

	public interface IntrinsicBridge {
		public Node unwrap(Node node);

		public Node wrap(Intrinsic intrinsic, Node node);
	}

	public static Map<String, Intrinsic> intrinsics = new HashMap<>();

	// Forces suspended node evaluation
	public static Intrinsic id_ = (bridge, inputs) -> inputs.get(0).finalNode();

	public static Node wrap(IntrinsicBridge bridge, Node node) {
		return bridge.wrap(id_, node);
	}

	static {
		for (Class<?> clazz : Arrays.asList( //
				ArrayIntrinsics.class //
				, BasicIntrinsics.class //
				, CharsIntrinsics.class //
				, MonadIntrinsics.class)) {
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
