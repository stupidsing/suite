package suite.jdk;

import static primal.statics.Fail.fail;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import primal.Verbs.Is;

public class Handler extends URLStreamHandler {

	public static void register() {
		var property = System.getProperty("java.protocol.handler.pkgs");
		var packageName = "suite";

		if (Is.blank(property))
			property = packageName;
		else if (!property.contains(packageName))
			property = packageName + "|" + property;

		System.setProperty("java.protocol.handler.pkgs", property);
	}

	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		var resource = getClass().getClassLoader().getResource(url.getPath());
		if (resource != null)
			return resource.openConnection();
		else
			return fail("resource not found: " + url);
	}

}
