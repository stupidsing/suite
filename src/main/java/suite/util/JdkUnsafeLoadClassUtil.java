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
		compile(packageName, className, java);
		return load(packageName, className);
	}

	private <T> Class<? extends T> load(String packageName, String className) throws IOException {
		LogUtil.info("Loading class " + className);

		String fullName = (!packageName.isEmpty() ? packageName + "." : "") + className;
		byte bytes[] = Files.readAllBytes(Paths.get(getBinDir() + "/" + fullName.replace(".", "/") + ".class"));
		@SuppressWarnings("unchecked")
		Class<? extends T> clazz = (Class<? extends T>) new UnsafeUtil().defineClass(fullName, bytes);
		return clazz;
	}

}
