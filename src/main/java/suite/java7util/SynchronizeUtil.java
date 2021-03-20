package suite.java7util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

@Deprecated
public class SynchronizeUtil {

	public static <I> I proxy(Class<I> interface_, I object) {
		@SuppressWarnings("unchecked")
		Class<I> clazz = (Class<I>) object.getClass();
		var classLoader = clazz.getClassLoader();
		Class<?>[] classes = { interface_ };

		InvocationHandler handler = (proxy, method, ps) -> {
			synchronized (object) {
				try {
					return method.invoke(object, ps);
				} catch (InvocationTargetException ite) {
					var th = ite.getTargetException();
					throw th instanceof Exception ex ? ex : ite;
				}
			}
		};

		@SuppressWarnings("unchecked")
		I proxied = (I) Proxy.newProxyInstance(classLoader, classes, handler);
		return proxied;
	}

}
