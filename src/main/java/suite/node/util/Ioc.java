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
		return of(clazz, true);
	}

	public static <T> T of(Class<T> clazz, boolean isSingleton) {
		var ioc = new Ioc();
		ioc.instances = singletonInstances;
		var t = ioc.instantiateIfRequired(clazz);
		if (!isSingleton)
			singletonInstances = ioc.instances;
		return t;
	}

	private <T> T instantiateIfRequired(Class<T> clazz) {
		var className = clazz.getCanonicalName();
		var instance0 = instances.get(className);
		Object instance1;
		if (instance0 != null)
			instance1 = instance0;
		else
			instances = instances.put(className, instance1 = instantiate(clazz));
		@SuppressWarnings("unchecked")
		var t = (T) instance1;
		return t;
	}

	private <T> Object instantiate(Class<T> clazz) {
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

		if (instance != null)
			return instance;
		else
			throw new RuntimeException("when instantiating " + clazz, exception);
	}

}
