package suite.util;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class JdkLoadClassUtil extends JdkUtil implements Closeable {

	private URLClassLoader classLoader;

	public JdkLoadClassUtil(String srcDir, String binDir) throws MalformedURLException {
		super(srcDir, binDir);
		classLoader = new URLClassLoader(new URL[] { new URL("file://" + binDir + "/") });

	}

	@Override
	public void close() throws IOException {
		classLoader.close();
	}

	public <T> T newInstance(Class<T> interfaceClazz, String packageName, String className, String java) throws IOException,
			ReflectiveOperationException {
		return compile(interfaceClazz, packageName, className, java).newInstance();
	}

	private <T> Class<? extends T> compile(Class<T> interfaceClazz, String packageName, String className, String java)
			throws IOException {
		compile(packageName, className, java);
		return load((!packageName.isEmpty() ? packageName + "." : "") + className);
	}

	private <T> Class<? extends T> load(String canonicalName) {
		LogUtil.info("Loading class " + canonicalName);

		try {
			@SuppressWarnings("unchecked")
			Class<? extends T> clazz = (Class<? extends T>) classLoader.loadClass(canonicalName);
			return clazz;
		} catch (ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		}
	}

}
