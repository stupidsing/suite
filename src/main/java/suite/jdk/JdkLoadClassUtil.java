package suite.jdk;

import static suite.util.Friends.rethrow;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

import suite.os.LogUtil;
import suite.util.Object_;
import suite.util.To;

public class JdkLoadClassUtil extends JdkUtil implements Closeable {

	private URLClassLoader classLoader;

	public JdkLoadClassUtil(Path srcDir, Path binDir) throws MalformedURLException {
		super(srcDir, binDir);
		classLoader = new URLClassLoader(new URL[] { To.url("file://" + binDir.toUri().toURL() + "/"), });

	}

	@Override
	public void close() throws IOException {
		classLoader.close();
	}

	public <T> T newInstance(Class<T> interfaceClazz, String canonicalName, String java) {
		compile(canonicalName, java);
		Class<? extends T> clazz = load(canonicalName);
		return Object_.new_(clazz);
	}

	private <T> Class<? extends T> load(String canonicalName) {
		LogUtil.info("Loading class " + canonicalName);

		return rethrow(() -> {
			@SuppressWarnings("unchecked")
			var clazz = (Class<? extends T>) classLoader.loadClass(canonicalName);
			return clazz;
		});
	}

}
