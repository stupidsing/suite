package suite.classpath;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import suite.util.String_;

public class Handler extends URLStreamHandler {

	public static void register() {
		String property = System.getProperty("java.protocol.handler.pkgs");
		String packageName = "suite";

		if (String_.isBlank(property))
			property = packageName;
		else if (!property.contains(packageName))
			property = packageName + "|" + property;

		System.setProperty("java.protocol.handler.pkgs", property);
	}

	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		URL resource = getClass().getClassLoader().getResource(url.getPath());
		if (resource != null)
			return resource.openConnection();
		else
			throw new RuntimeException("Resource not found: " + url);
	}

}
