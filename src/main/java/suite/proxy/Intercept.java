package suite.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import primal.fp.Funs.Fun;

public class Intercept {

	public interface Invocation {
		public Object invoke(Method method, Object[] ps) throws Exception;
	}

	public static <I> I object(Class<I> interface_, I object, Fun<Invocation, Invocation> fun) {
		@SuppressWarnings("unchecked")
		var clazz = (Class<I>) object.getClass();
		var classLoader = clazz.getClassLoader();
		Class<?>[] classes = { interface_, };

		InvocationHandler handler = (proxy, method, parameters) -> {
			try {
				Invocation invocation0 = (m, ps) -> m.invoke(object, ps);
				var invocation1 = fun.apply(invocation0);
				return invocation1.invoke(method, parameters);
			} catch (InvocationTargetException ite) {
				var th = ite.getTargetException();
				throw th instanceof Exception ? (Exception) th : ite;
			}
		};

		@SuppressWarnings("unchecked")
		var proxied = (I) Proxy.newProxyInstance(classLoader, classes, handler);
		return proxied;
	}

}
