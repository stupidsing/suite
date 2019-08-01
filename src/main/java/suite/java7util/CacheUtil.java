package suite.java7util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import primal.Verbs.Equals;
import primal.Verbs.Get;
import primal.fp.Funs.Fun;

@Deprecated
public class CacheUtil {

	private Map<Key, Object> results = new ConcurrentHashMap<>();

	private static class Key {
		private Object bean;
		private Method method;
		private Object[] arguments;

		public Key(Object bean, Method method, Object[] arguments) {
			this.bean = bean;
			this.method = method;
			this.arguments = arguments;
		}

		public boolean equals(Object object) {
			if (Get.clazz(object) == Key.class) {
				var other = (Key) object;
				return bean == other.bean //
						&& Equals.ab(method, other.method) //
						&& Arrays.deepEquals(arguments, other.arguments);
			} else
				return false;
		}

		public int hashCode() {
			var h = 7;
			h = h * 31 + System.identityHashCode(bean);
			h = h * 31 + Objects.hashCode(method);
			h = h * 31 + Arrays.deepHashCode(arguments);
			return h;
		}
	}

	public <I, O> Fun<I, O> proxy(Fun<I, O> fun) {
		@SuppressWarnings("unchecked")
		Fun<I, O> proxy = proxy(Fun.class, fun);
		return proxy;
	}

	public <I> I proxy(Class<I> interface_, I object) {
		return proxy(interface_, object, new HashSet<>(List.of(interface_.getMethods())));
	}

	public <I> I proxyByMethodNames(Class<I> interface_, I object, Set<String> methodNames) {
		var methods = new HashSet<Method>();

		for (var method : interface_.getMethods())
			if (methodNames.contains(method.getName()))
				methods.add(method);

		return proxy(interface_, object, methods);
	}

	public <I> I proxy(Class<I> interface_, I object, Collection<Method> methods) {
		InvocationHandler handler = (proxy, method, ps) -> {
			Key key = methods.contains(method) ? new Key(object, method, ps) : null;
			var isCached = key != null && results.containsKey(key);
			Object result;

			if (!isCached)
				try {
					results.put(key, result = method.invoke(object, ps));
				} catch (InvocationTargetException ite) {
					var th = ite.getTargetException();
					throw th instanceof Exception ? (Exception) th : ite;
				}
			else
				result = results.get(key);

			return result;
		};

		@SuppressWarnings("unchecked")
		Class<I> clazz = (Class<I>) object.getClass();
		var classLoader = clazz.getClassLoader();
		Class<?>[] classes = { interface_ };

		@SuppressWarnings("unchecked")
		I proxied = (I) Proxy.newProxyInstance(classLoader, classes, handler);
		return proxied;
	}

}
