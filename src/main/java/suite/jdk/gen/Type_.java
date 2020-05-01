package suite.jdk.gen;

import org.apache.bcel.Const;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;
import primal.Verbs.Equals;

import java.util.Arrays;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

public class Type_ {

	public static Class<?> classOf(Type type) {
		String className;

		if (type instanceof ObjectType)
			className = ((ObjectType) type).getClassName();
		else if (type instanceof BasicType)
			className = Const.getTypeName(((BasicType) type).getType());
		else
			return fail();

		return ex(() -> {
			for (var clazz : Arrays.asList(
					byte.class,
					boolean.class,
					char.class,
					double.class,
					float.class,
					int.class,
					long.class,
					short.class,
					void.class))
				if (Equals.string(className, clazz.getSimpleName()))
					return clazz;
			return Class.forName(className);
		});
	}

}
