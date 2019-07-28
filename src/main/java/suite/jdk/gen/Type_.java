package suite.jdk.gen;

import static suite.util.Fail.fail;
import static suite.util.Friends.rethrow;

import java.util.Arrays;

import org.apache.bcel.Const;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import suite.util.String_;

public class Type_ {

	public static Class<?> classOf(Type type) {
		String className;

		if (type instanceof ObjectType)
			className = ((ObjectType) type).getClassName();
		else if (type instanceof BasicType)
			className = Const.getTypeName(((BasicType) type).getType());
		else
			return fail();

		return rethrow(() -> {
			for (var clazz : Arrays.asList( //
					byte.class, //
					boolean.class, //
					char.class, //
					double.class, //
					float.class, //
					int.class, //
					long.class, //
					short.class, //
					void.class))
				if (String_.equals(className, clazz.getSimpleName()))
					return clazz;
			return Class.forName(className);
		});
	}

}
