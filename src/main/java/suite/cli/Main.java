package suite.cli;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import primal.Nouns.Utf8;
import primal.Verbs.Close;
import primal.Verbs.Equals;
import primal.Verbs.ReadString;
import primal.Verbs.Take;
import primal.os.Log_;
import suite.Suite;
import suite.net.ServeSocket;
import suite.util.ParseUtil;
import suite.util.RunUtil;

/**
 * Logic interpreter and functional interpreter. Likes Prolog and Haskell.
 *
 * @author ywsing
 */
// mvn compile exec:java -Dexec.mainClass=suite.cli.Main
public class Main implements AutoCloseable {

	private CommandOptions opt;
	private CommandDispatcher dispatcher;

	private Reader reader = new BufferedReader(new InputStreamReader(System.in, Utf8.charset));
	private Writer writer = new BufferedWriter(new OutputStreamWriter(System.out, Utf8.charset));

	public static void main(String[] args) {
		RunUtil.run(() -> {
			try (var main = new Main()) {
				return main.run(args);
			}
		});
	}

	private boolean run(String[] args) throws IOException {
		opt = new CommandOptions();

		var b = true;
		var inputs = new ArrayList<String>();
		var source = Take.from(args);
		String verb = null;
		String arg;

		while ((arg = source.g()) != null)
			if (arg.startsWith("-"))
				if (Equals.string(arg, "--file"))
					inputs.add(readScript(source.g()));
				else
					b &= opt.processOption(arg, source);
			else if (verb == null)
				verb = arg;
			else
				inputs.add(arg);

		dispatcher = new CommandDispatcher(opt);

		if (b)
			if (Equals.string(verb, "evaluate"))
				b &= dispatcher.dispatchEvaluate(inputs);
			else if (Equals.string(verb, "filter"))
				b &= dispatcher.dispatchFilter(inputs, reader, writer);
			else if (Equals.string(verb, "precompile"))
				b &= dispatcher.dispatchPrecompile(inputs);
			else if (Equals.string(verb, "prove"))
				b &= dispatcher.dispatchProve(inputs);
			else if (Equals.string(verb, "query"))
				b &= runInteractive(inputs);
			else if (Equals.string(verb, "serve"))
				new ServeSocket().run();
			else if (verb != null && verb.startsWith("suite.")) {
				var verb_ = verb;
				ex(() -> {
					var c = Class.forName(verb_);
					return c.getMethod("main", String[].class).invoke(null);
				});
			} else if (Equals.string(verb, "type"))
				b &= dispatcher.dispatchType(inputs);
			else if (verb == null)
				b &= runInteractive(inputs);
			else
				fail("unknown action " + verb);

		return b;
	}

	private String readScript(String filename) {
		var contents = ReadString.from(filename);
		if (contents.startsWith("#")) // skips first line comment
			contents = contents.substring(contents.indexOf('\n') + 1);
		return contents;
	}

	private boolean runInteractive(List<String> filenames) throws IOException {
		var br = new BufferedReader(reader);
		var code = true;
		String ready;

		code &= dispatcher.importFiles(filenames);

		try (var sw = new StringWriter()) {
			var node = Suite.applyWriter(Suite.parse("\"READY\""));
			Suite.evaluateFunToWriter(opt.fcc(node), sw);
			ready = sw.toString();
		} catch (Exception ex) {
			Log_.error(ex);
			ready = "ERROR";
		}

		opt.prompt().println(ready);

		while (true)
			try {
				var sb = new StringBuilder();
				String line;

				do {
					opt.prompt().print(sb.length() == 0 ? "=> " : "   ");

					if ((line = br.readLine()) != null)
						sb.append(line + "\n");
					else
						return code;
				} while (!opt.isQuiet() //
						&& (!ParseUtil.isParseable(sb.toString(), true) || !line.isEmpty() && !line.endsWith("#")));

				code &= dispatcher.dispatchCommand(sb.toString(), writer);
			} catch (Throwable ex) {
				Log_.error(ex);
			}
	}

	@Override
	public void close() {
		Close.quietly(reader, writer);
	}

}
