package suite.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import primal.Ob;
import suite.streamlet.Read;

public class Cache {

	private Map<Key, Object> results = new ConcurrentHashMap<>();

	private class Key {
		private Object bean;
		private Method method;
		private Object[] arguments;

		public Key(Object bean, Method method, Object[] arguments) {
			this.bean = bean;
			this.method = method;
			this.arguments = arguments;
		}

		public boolean equals(Object object) {
			if (Ob.clazz(object) == Key.class) {
				var other = (Key) object;
				return bean == other.bean //
						&& Ob.equals(method, other.method) //
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

	public <I> I proxy(Class<I> interface_, I object) {
		return proxy(interface_, object, new HashSet<>(List.of(interface_.getMethods())));
	}

	public <I> I proxyByMethodNames(Class<I> interface_, I object, Set<String> methodNames) {
		var methods = Read //
				.from(interface_.getMethods()) //
				.filter(m -> methodNames.contains(m.getName())) //
				.toSet();
		return proxy(interface_, object, methods);
	}

	public <I> I proxy(Class<I> interface_, I object, Collection<Method> methods) {
		return Intercept.object(interface_, object, invocation -> (m, ps) -> {
			var key = methods.contains(m) ? new Key(object, m, ps) : null;
			var isCached = key != null && results.containsKey(key);
			Object result;

			if (!isCached)
				try {
					results.put(key, result = invocation.invoke(m, ps));
				} catch (InvocationTargetException ite) {
					var th = ite.getTargetException();
					throw th instanceof Exception ? (Exception) th : ite;
				}
			else
				result = results.get(key);

			return result;
		});
	}

}
