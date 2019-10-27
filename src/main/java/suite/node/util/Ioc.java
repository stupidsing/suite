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
		var instantiate = new Ioc();
		instantiate.instances = singletonInstances;
		var t = instantiate.instantiate(clazz);
		if (!isSingleton)
			singletonInstances = instantiate.instances;
		return t;
	}

	private <T> T instantiate(Class<T> clazz) {
		var className = clazz.getCanonicalName();
		var instance = instances.get(className);

		if (instance == null) {
			Exception exception = null;

			for (var ctor : clazz.getConstructors())
				try {
					instance = ctor.newInstance(Read //
							.from(ctor.getParameters()) //
							.map(parameter -> (Object) instantiate(parameter.getType())) //
							.toArray(Object.class));

					break;
				} catch (Exception ex) {
					exception = ex;
				}

			if (instance != null)
				instances = instances.put(className, instance);
			else
				throw new RuntimeException("when instantiating " + clazz, exception);
		}

		@SuppressWarnings("unchecked")
		var t = (T) instance;
		return t;
	}

}
