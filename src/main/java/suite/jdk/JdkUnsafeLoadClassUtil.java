package suite.jdk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import suite.os.LogUtil;
import suite.util.Object_;

public class JdkUnsafeLoadClassUtil extends JdkUtil {

	public JdkUnsafeLoadClassUtil(Path srcDir, Path binDir) {
		super(srcDir, binDir);
	}

	public <T> T newInstance(Class<T> interfaceClazz, String canonicalName, String java) throws IOException {
		return Object_.new_(load(interfaceClazz, canonicalName, compile(canonicalName, java)));
	}

	private <T> Class<? extends T> load(Class<T> interfaceClazz, String canonicalName, Path path) throws IOException {
		LogUtil.info("Loading class " + canonicalName);
		var bytes = Files.readAllBytes(path);
		return new UnsafeUtil().defineClass(interfaceClazz, canonicalName, bytes);
	}

}
