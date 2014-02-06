package suite.cli;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import suite.Suite;
import suite.editor.Editor;
import suite.fp.FunCompilerConfig;
import suite.lp.kb.RuleSet;
import suite.util.FunUtil.Source;

/**
 * Command line interface environment.
 * 
 * @author ywsing
 */
public class CliConfig {

	private FunCompilerConfig fcc = new FunCompilerConfig();

	private boolean isQuiet = false;
	private boolean isFilter = false;
	private boolean isFunctional = false;
	private boolean isLogical = false;

	public CliConfig() throws IOException {
		RuleSet ruleSet = getRuleSet();
		Suite.importResource(ruleSet, "auto.sl");
	}

	public boolean processOption(String arg, Source<String> source) {
		return processOption(arg, source, true);
	}

	public boolean processOption(String arg, Source<String> source, boolean on) {
		boolean result = true;
		String arg1;

		if (arg.equals("-dump-code"))
			fcc.setDumpCode(on);
		else if (arg.equals("-eager"))
			fcc.setLazy(!on);
		else if (arg.equals("-editor"))
			new Editor().open();
		else if (arg.equals("-filter"))
			isFilter = on;
		else if (arg.equals("-functional"))
			isFunctional = on;
		else if (arg.equals("-lazy"))
			fcc.setLazy(on);
		else if (arg.equals("-libraries") && (arg1 = source.source()) != null)
			fcc.setLibraries(Arrays.asList(arg1.split(",")));
		else if (arg.equals("-logical"))
			isLogical = on;
		else if (arg.startsWith("-no-"))
			result &= processOption("-" + arg.substring(4), source, false);
		else if (arg.equals("-precompile") && (arg1 = source.source()) != null)
			for (String lib : arg1.split(","))
				result &= Suite.precompile(lib, fcc.getProverConfig());
		else if (arg.equals("-quiet"))
			isQuiet = on;
		else if (arg.equals("-trace"))
			fcc.getProverConfig().setTrace(on);
		else
			throw new RuntimeException("Unknown option " + arg);

		return result;
	}

	public boolean importFile(List<String> importFilenames) throws IOException {
		boolean code = true;
		RuleSet ruleSet = getRuleSet();
		for (String importFilename : importFilenames)
			code &= Suite.importFile(ruleSet, importFilename);
		return code;
	}

	public PrintStream prompt() {
		try (OutputStream os = new OutputStream() {
			public void write(int c) {
			}
		}) {
			return !isQuiet ? System.out : new PrintStream(os);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private RuleSet getRuleSet() {
		return fcc.getProverConfig().ruleSet();
	}

	public FunCompilerConfig getFcc() {
		return fcc;
	}

	public boolean isQuiet() {
		return isQuiet;
	}

	public boolean isFilter() {
		return isFilter;
	}

	public boolean isFunctional() {
		return isFunctional;
	}

	public boolean isLogical() {
		return isLogical;
	}

}
