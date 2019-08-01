package suite.jdk;

import static primal.statics.Rethrow.ex;

import java.nio.file.Files;
import java.nio.file.Path;

import primal.Verbs.New;
import primal.os.Log_;

public class JdkUnsafeLoadClassUtil extends JdkUtil {

	public JdkUnsafeLoadClassUtil(Path srcDir, Path binDir) {
		super(srcDir, binDir);
	}

	public <T> T newInstance(Class<T> interfaceClazz, String canonicalName, String java) {
		return New.clazz(load(interfaceClazz, canonicalName, compile(canonicalName, java)));
	}

	private <T> Class<? extends T> load(Class<T> interfaceClazz, String canonicalName, Path path) {
		Log_.info("Loading class " + canonicalName);
		var bytes = ex(() -> Files.readAllBytes(path));
		return new UnsafeUtil().defineClass(interfaceClazz, canonicalName, bytes);
	}

}
