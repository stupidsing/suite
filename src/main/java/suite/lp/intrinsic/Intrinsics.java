package suite.lp.intrinsic;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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

	static {
		for (Class<?> clazz : Arrays.asList( //
				ArrayIntrinsics.class //
				, BasicIntrinsics.class //
				, CharsIntrinsics.class //
				, MonadIntrinsics.class))
			for (Field field : clazz.getFields())
				if (Modifier.isStatic(field.getModifiers()) && Intrinsic.class.isAssignableFrom(field.getType()))
					try {
						intrinsics.put(clazz.getName() + field.getName(), (Intrinsic) field.get(null));
					} catch (Exception ex) {
						LogUtil.error(ex);
					}
	}

}
