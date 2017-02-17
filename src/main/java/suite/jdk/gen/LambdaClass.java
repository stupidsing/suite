package suite.jdk.gen;

import java.lang.reflect.Method;

import suite.streamlet.Read;
import suite.util.Rethrow;
import suite.util.Util;

public class LambdaClass<I> {

	public final Class<I> interfaceClass;
	public final String methodName;

	public static <I> LambdaClass<I> of(Class<I> interfaceClass) {
		return of(interfaceClass, Type_.methodOf(interfaceClass).getName());
	}

	public static <I> LambdaClass<I> of(Class<I> interfaceClass, String methodName) {
		return new LambdaClass<>(interfaceClass, methodName);
	}

	protected LambdaClass(Class<I> interfaceClass, String methodName) {
		this.interfaceClass = interfaceClass;
		this.methodName = methodName;
	}

	public Method method() {
		Method methods[] = Rethrow.reflectiveOperationException(() -> interfaceClass.getMethods());
		return Read.from(methods).filter(m -> Util.stringEquals(m.getName(), methodName)).uniqueResult();
	}

}
