package suite.jdk.gen;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.util.Arrays;

import org.apache.bcel.Const;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import primal.Verbs.Equals;

public class Type_ {

	public static Class<?> classOf(Type type) {
		String className;

		if (type instanceof ArrayType type_)
			if (type_.getElementType() instanceof BasicType elementType_)
				return switch (Const.getTypeName(elementType_.getType())) {
				case "byte" -> byte[].class;
				case "boolean" -> boolean[].class;
				case "char" -> char[].class;
				case "double" -> double[].class;
				case "float" -> float[].class;
				case "int" -> int[].class;
				case "long" -> long[].class;
				case "short" -> short[].class;
				default -> throw new RuntimeException();
				};
			else
				return Object[].class;
		else if (type instanceof ObjectType type_)
			className = type_.getClassName();
		else if (type instanceof BasicType type_)
			className = Const.getTypeName(type_.getType());
		else
			return fail();

		return ex(() -> {
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
				if (Equals.string(className, clazz.getSimpleName()))
					return clazz;
			return Class.forName(className);
		});
	}

}
