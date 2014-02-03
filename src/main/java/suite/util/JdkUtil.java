package suite.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
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
	private URLClassLoader ucl;

	public JdkUtil(String tmpDir) throws MalformedURLException {
		this(tmpDir, tmpDir);
	}

	public JdkUtil(String srcDir, String binDir) throws MalformedURLException {
		this.srcDir = srcDir;
		this.binDir = binDir;
		ucl = new URLClassLoader(new URL[] { new URL("file://" + binDir + "/") });

	}

	@Override
	public void close() throws IOException {
		ucl.close();
	}

	public <T> Class<? extends T> compile(Class<T> interfaceClazz, String java, String packageName, String className)
			throws MalformedURLException, IOException {
		compile(java, packageName, className);
		return load(interfaceClazz, packageName, className);
	}

	private void compile(String java, String packageName, String className) throws IOException {
		String pathName = srcDir + "/" + packageName.replace('.', '/');
		String filename = pathName + "/" + className + ".java";
		new File(pathName).mkdirs();

		LogUtil.info("Writing " + filename);
		try (OutputStream os = new FileOutputStream(filename)) {
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

	private <T> Class<? extends T> load(Class<T> interfaceClazz, String packageName, String className) {
		LogUtil.info("Loading class " + className);

		try {
			String fullName = (!packageName.isEmpty() ? packageName + "." : "") + className;

			@SuppressWarnings("unchecked")
			Class<? extends T> clazz = (Class<? extends T>) ucl.loadClass(fullName);
			return clazz;
		} catch (ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		}
	}

}
