package suite.sample;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import suite.util.String_;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

/**
 * Starts program using a JavaScript script. Perhaps you can avoid using Spring
 * XML.
 *
 * @author ywsing
 */
public class JsMain extends ExecutableProgram {

	private List<String> defaultJsFiles = Arrays.asList("conf/loader.js");

	private ScriptEngine engine = new ScriptEngineManager().getEngineByExtension("js");

	public static void main(String[] args) {
		Util.run(JsMain.class, args);
	}

	protected synchronized boolean run(String[] args) throws IOException, ScriptException {
		List<String> filenames = new ArrayList<>();

		for (String arg : args)
			if (!arg.isEmpty())
				if (arg.charAt(0) == '-')
					engine.eval(arg.substring(1));
				else
					filenames.add(arg);

		if (filenames.size() == 0)
			filenames = defaultJsFiles;

		boolean result = true;

		for (String filename : filenames) {
			Object r = engine.eval(new FileReader(filename));
			result &= r == Boolean.TRUE //
					|| r instanceof Number && ((Number) r).intValue() != 0 //
					|| r instanceof String && String_.isNotBlank((String) r);
		}

		return result;
	}

}
