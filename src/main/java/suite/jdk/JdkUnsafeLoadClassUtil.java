package suite.jdk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import suite.os.LogUtil;

public class JdkUnsafeLoadClassUtil extends JdkUtil {

	public JdkUnsafeLoadClassUtil(String srcDir, String binDir) {
		super(srcDir, binDir);
	}

	public <T> T newInstance(Class<T> interfaceClazz, String canonicalName, String java) throws IOException,
			ReflectiveOperationException {
		String classFilename = compile(canonicalName, java);
		Class<? extends T> clazz = load(interfaceClazz, canonicalName, classFilename);
		return clazz.newInstance();
	}

	private <T> Class<? extends T> load(Class<T> interfaceClazz, String canonicalName, String classFilename) throws IOException {
		LogUtil.info("Loading class " + canonicalName);
		byte bytes[] = Files.readAllBytes(Paths.get(classFilename));
		return new UnsafeUtil().defineClass(interfaceClazz, canonicalName, bytes);
	}

}
