package suite.jdk;

import primal.Verbs.New;
import primal.Verbs.ReadFile;
import primal.jdk.UnsafeUtil;
import primal.os.Log_;

import java.nio.file.Path;

public class JdkUnsafeLoadClassUtil extends JdkUtil {

	public JdkUnsafeLoadClassUtil(Path srcDir, Path binDir) {
		super(srcDir, binDir);
	}

	public <T> T newInstance(Class<T> interfaceClazz, String canonicalName, String java) {
		return New.clazz(load(interfaceClazz, canonicalName, compile(canonicalName, java)));
	}

	private <T> Class<? extends T> load(Class<T> interfaceClazz, String canonicalName, Path path) {
		Log_.info("Loading class " + canonicalName);
		var bytes = ReadFile.from(path).readBytes();
		return new UnsafeUtil().defineClass(interfaceClazz, canonicalName, bytes);
	}

}
