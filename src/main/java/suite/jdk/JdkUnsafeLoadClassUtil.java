package suite.jdk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import suite.os.LogUtil;

public class JdkUnsafeLoadClassUtil extends JdkUtil {

	public JdkUnsafeLoadClassUtil(Path srcDir, Path binDir) {
		super(srcDir, binDir);
	}

	public <T> T newInstance(Class<T> interfaceClazz, String canonicalName, String java)
			throws IOException, ReflectiveOperationException {
		Path path = compile(canonicalName, java);
		Class<? extends T> clazz = load(interfaceClazz, canonicalName, path);
		return clazz.newInstance();
	}

	private <T> Class<? extends T> load(Class<T> interfaceClazz, String canonicalName, Path path) throws IOException {
		LogUtil.info("Loading class " + canonicalName);
		byte bytes[] = Files.readAllBytes(path);
		return new UnsafeUtil().defineClass(interfaceClazz, canonicalName, bytes);
	}

}
