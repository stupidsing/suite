package suite.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class JdkUtil implements Closeable {

	private String srcDir;
	private String binDir;
	private URLClassLoader classLoader;

	public JdkUtil(String tmpDir) throws MalformedURLException {
		this(tmpDir, tmpDir);
	}

	public JdkUtil(String srcDir, String binDir) throws MalformedURLException {
		this.srcDir = srcDir;
		this.binDir = binDir;
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
		return load(packageName, className);
	}

	private void compile(String packageName, String className, String java) throws IOException {
		String pathName = srcDir + "/" + packageName.replace('.', '/');
		String filename = pathName + "/" + className + ".java";

		LogUtil.info("Writing " + filename);
		try (OutputStream os = FileUtil.out(new File(filename))) {
			os.write(java.getBytes(FileUtil.charset));
		}
		File file = new File(filename);

		// Compile the Java, load the class, return an instantiated object
		LogUtil.info("Compiling " + file);
		new File(binDir).mkdirs();

		JavaCompiler jc = ToolProvider.getSystemJavaCompiler();

		try (StandardJavaFileManager sjfm = jc.getStandardFileManager(null, null, null)) {
			if (!jc.getTask(null //
					, null //
					, null //
					, Arrays.asList("-d", binDir) //
					, null //
					, sjfm.getJavaFileObjects(file)).call())
				throw new RuntimeException("Java compilation error");
		}
	}

	private <T> Class<? extends T> load(String packageName, String className) {
		LogUtil.info("Loading class " + className);

		try {
			String fullName = (!packageName.isEmpty() ? packageName + "." : "") + className;

			@SuppressWarnings("unchecked")
			Class<? extends T> clazz = (Class<? extends T>) classLoader.loadClass(fullName);
			return clazz;
		} catch (ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		}
	}

}
