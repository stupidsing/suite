package suite.jdk.lambda;

import static suite.util.Friends.rethrow;

import java.lang.reflect.Method;

import suite.adt.pair.Fixie_.FixieFun2;
import suite.streamlet.Read;
import suite.util.String_;
import suite.util.Util;

public class LambdaInterface<I> {

	public final Class<I> interfaceClass;
	public final String methodName;

	private Method method;

	public static <I> LambdaInterface<I> of(Class<I> interfaceClass) {
		return of(interfaceClass, Util.methodOf(interfaceClass).getName());
	}

	public static <I> LambdaInterface<I> of(Class<I> interfaceClass, String methodName) {
		return new LambdaInterface<>(interfaceClass, methodName);
	}

	private LambdaInterface(Class<I> interfaceClass, String methodName) {
		this.interfaceClass = interfaceClass;
		this.methodName = methodName;
	}

	public Method method() {
		if (method == null) {
			var methods = rethrow(interfaceClass::getMethods);
			method = Read.from(methods).filter(m -> String_.equals(m.getName(), methodName)).uniqueResult();
		}
		return method;
	}

	public <R> R apply(FixieFun2<Class<I>, String, R> fun) {
		return fun.apply(interfaceClass, methodName);
	}

}
