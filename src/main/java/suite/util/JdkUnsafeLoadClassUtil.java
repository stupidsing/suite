package suite.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class JdkUnsafeLoadClassUtil extends JdkUtil {

	public JdkUnsafeLoadClassUtil(String srcDir, String binDir) {
		super(srcDir, binDir);
	}

	public <T> T newInstance(Class<T> interfaceClazz, String packageName, String className, String java) throws IOException,
			ReflectiveOperationException {
		return compile(interfaceClazz, packageName, className, java).newInstance();
	}

	private <T> Class<? extends T> compile(Class<T> interfaceClazz, String packageName, String className, String java)
			throws IOException {
		String canonicalName = (!packageName.isEmpty() ? packageName + "." : "") + className;
		String classFilename = compile(packageName, className, java);
		return load(canonicalName, classFilename);
	}

	private <T> Class<? extends T> load(String canonicalName, String classFilename) throws IOException {
		LogUtil.info("Loading class " + canonicalName);
		byte bytes[] = Files.readAllBytes(Paths.get(classFilename));
		@SuppressWarnings("unchecked")
		Class<? extends T> clazz = (Class<? extends T>) new UnsafeUtil().defineClass(canonicalName, bytes);
		return clazz;
	}

}
