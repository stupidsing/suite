package suite.proxy;


public class Synchronize {

	public static <I> I proxy(Class<I> interface_, I object) {
		return ProxyUtil.proxy(interface_, object, invocation -> (m, ps) -> {
			synchronized (object) {
				return invocation.invoke(m, ps);
			}
		});
	}

}
