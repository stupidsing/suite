package suite.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import suite.util.FileUtil;
import suite.util.FunUtil.Source;
import suite.util.LogUtil;
import suite.util.ParseUtil;
import suite.util.To;
import suite.util.Util;

/**
 * Logic interpreter and functional interpreter. Likes Prolog and Haskell.
 * 
 * @author ywsing
 */
public class Main implements AutoCloseable {

	private CommandOption cfg;
	private CommandDispatcher dispatcher;

	private Reader reader = new InputStreamReader(System.in, FileUtil.charset);
	private Writer writer = new OutputStreamWriter(System.out, FileUtil.charset);

	public static void main(String args[]) {
		int code;

		try (Main main = new Main()) {
			try {
				code = main.run(args);
			} catch (Throwable ex) {
				LogUtil.error(ex);
				code = 1;
			}
		}

		System.exit(code);
	}

	private int run(String args[]) throws IOException {
		cfg = new CommandOption();
		dispatcher = new CommandDispatcher(cfg);

		boolean result = true;
		List<String> inputs = new ArrayList<>();
		Source<String> source = To.source(args);
		String arg;

		while ((arg = source.source()) != null)
			if (arg.startsWith("-"))
				result &= cfg.processOption(arg, source);
			else
				inputs.add(arg);

		if (result)
			if (cfg.isFilter()) // Inputs as program
				result &= dispatcher.dispatchFilter(inputs, reader, writer);
			else if (cfg.isFunctional()) // Inputs as files
				result &= dispatcher.dispatchFunctional(inputs);
			else if (cfg.isLogical())
				result &= dispatcher.dispatchLogical(inputs); // Inputs as files
			else
				// Inputs as files
				result &= run(inputs);

		return result ? 0 : 1;
	}

	private boolean run(List<String> importFilenames) throws IOException {
		BufferedReader br = new BufferedReader(reader);
		boolean code = true;

		code &= cfg.importFile(importFilenames);
		cfg.prompt().println("READY");

		while (true)
			try {
				StringBuilder sb = new StringBuilder();
				String line;

				do {
					cfg.prompt().print(sb.length() == 0 ? "=> " : "   ");

					if ((line = br.readLine()) != null)
						sb.append(line + "\n");
					else
						return code;
				} while (!cfg.isQuiet() //
						&& (!ParseUtil.isParseable(sb.toString()) || !line.isEmpty() && !line.endsWith("#")));

				code &= dispatcher.dispatchCommand(sb.toString(), writer);
			} catch (Throwable ex) {
				LogUtil.error(ex);
			}
	}

	@Override
	public void close() {
		Util.closeQuietly(reader);
		Util.closeQuietly(writer);
	}

}
