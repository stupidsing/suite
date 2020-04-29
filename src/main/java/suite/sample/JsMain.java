package suite.sample;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import primal.Verbs.Is;
import suite.util.RunUtil;

/**
 * Starts program using a JavaScript script. Perhaps you can avoid using Spring
 * XML.
 *
 * @author ywsing
 */
public class JsMain {

	private List<String> defaultJsFiles = List.of("conf/loader.js");

	private ScriptEngine engine = new ScriptEngineManager().getEngineByExtension("js");

	public static void main(String[] args) {
		RunUtil.run(() -> new JsMain().run(args));
	}

	private synchronized boolean run(String[] args) throws IOException, ScriptException {
		List<String> filenames = new ArrayList<>();

		for (var arg : args)
			if (!arg.isEmpty())
				if (arg.charAt(0) == '-')
					engine.eval(arg.substring(1));
				else
					filenames.add(arg);

		if (filenames.size() == 0)
			filenames = defaultJsFiles;

		var b = true;

		for (var filename : filenames) {
			var r = engine.eval(new FileReader(filename));
			b &= r == Boolean.TRUE
					|| r instanceof Number && ((Number) r).intValue() != 0
					|| r instanceof String && Is.notBlank((String) r);
		}

		return b;
	}

}
