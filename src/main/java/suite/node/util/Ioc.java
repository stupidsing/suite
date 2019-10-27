package suite.node.util;

import primal.MoreVerbs.Read;
import primal.persistent.PerMap;

/**
 * Inversion of control. Reads dependency injection.
 *
 * @author ywsing
 */
public class Ioc {

	private static PerMap<String, Object> singletonInstances = PerMap.empty();

	private PerMap<String, Object> instances;

	public static <T> T of(Class<T> clazz) {
		return new Ioc(singletonInstances).instantiate(clazz);
	}

	public static <T> T ofNew(Class<T> clazz) {
		var ioc = new Ioc(singletonInstances);
		var t = ioc.instantiateIfRequired(clazz);
		singletonInstances = ioc.instances;
		return t;
	}

	public Ioc(PerMap<String, Object> instances) {
		this.instances = instances;
	}

	private <T> T instantiateIfRequired(Class<T> clazz) {
		var className = clazz.getCanonicalName();
		var instance = instances.get(className);
		if (instance != null) {
			@SuppressWarnings("unchecked")
			var t = (T) instance;
			return t;
		} else {
			var t = instantiate(clazz);
			instances = instances.put(className, t);
			return t;
		}
	}

	private <T> T instantiate(Class<T> clazz) {
		Object instance = null;
		Exception exception = null;

		for (var ctor : clazz.getConstructors())
			try {
				instance = ctor.newInstance(Read //
						.from(ctor.getParameters()) //
						.map(parameter -> (Object) instantiateIfRequired(parameter.getType())) //
						.toArray(Object.class));

				break;
			} catch (Exception ex) {
				exception = ex;
			}

		if (instance != null) {
			@SuppressWarnings("unchecked")
			var t = (T) instance;
			return t;
		} else
			throw new RuntimeException("when instantiating " + clazz, exception);
	}

}
