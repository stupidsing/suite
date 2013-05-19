package org.suite;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.log4j.Level;
import org.util.LogUtil;

/**
 * Starts program using a JavaScript script. Perhaps you can avoid using Spring
 * XML.
 * 
 * @author ywsing
 */
public class JsMain {

	private final List<String> defaultJsFiles = Arrays.asList("conf/loader.js");

	private final ScriptEngine engine = new ScriptEngineManager()
			.getEngineByExtension("js");

	public static void main(String args[]) {
		instance.run(args);
	}

	public synchronized void run(String args[]) {
		LogUtil.initLog4j(Level.INFO);

		try {
			List<String> filenames = new ArrayList<>();

			for (String arg : args)
				if (!arg.equals(""))
					if (arg.charAt(0) == '-')
						engine.eval(arg.substring(1));
					else
						filenames.add(arg);

			if (filenames.size() == 0)
				filenames = defaultJsFiles;

			for (String filename : filenames)
				engine.eval(new FileReader(filename));

			wait();
		} catch (Throwable ex) {
			LogUtil.fatal(ex);
			ex.printStackTrace();
		}
	}

	private JsMain() { // Singleton; disallow other instances
	}

	public static JsMain getInstance() {
		return instance;
	}

	private static JsMain instance = new JsMain();

	public ScriptEngine getScriptEngine() {
		return engine;
	}

}
