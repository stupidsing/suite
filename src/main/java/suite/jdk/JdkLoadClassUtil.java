package suite.jdk;

import static primal.statics.Rethrow.ex;

import java.io.Closeable;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

import primal.Verbs.Close;
import primal.Verbs.New;
import primal.os.Log_;
import suite.util.To;

public class JdkLoadClassUtil extends JdkUtil implements Closeable {

	private URLClassLoader classLoader;

	public JdkLoadClassUtil(Path srcDir, Path binDir) {
		super(srcDir, binDir);
		classLoader = ex(() -> new URLClassLoader(new URL[] { To.url("file://" + binDir.toUri().toURL() + "/"), }));

	}

	@Override
	public void close() {
		Close.quietly(classLoader);
	}

	public <T> T newInstance(Class<T> interfaceClazz, String canonicalName, String java) {
		compile(canonicalName, java);
		Class<? extends T> clazz = load(canonicalName);
		return New.clazz(clazz);
	}

	private <T> Class<? extends T> load(String canonicalName) {
		Log_.info("Loading class " + canonicalName);

		return ex(() -> {
			@SuppressWarnings("unchecked")
			var clazz = (Class<? extends T>) classLoader.loadClass(canonicalName);
			return clazz;
		});
	}

}
