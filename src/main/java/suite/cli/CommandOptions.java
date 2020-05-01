package suite.cli;

import primal.Verbs.Concat;
import primal.Verbs.Equals;
import primal.fp.Funs.Source;
import suite.Suite;
import suite.editor.EditorMain;
import suite.fp.FunCompilerCfg;
import suite.lp.Configuration.ProverCfg;
import suite.lp.kb.RuleSet;
import suite.node.Node;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static primal.statics.Fail.fail;

/**
 * Command line interface environment.
 *
 * @author ywsing
 */
public class CommandOptions {

	// program type options
	private boolean isChars = false;
	private boolean isDo = false;
	private boolean isQuiet = false;

	// program evaluation options
	private boolean isLazy = true;
	private List<String> imports = List.of();
	private List<String> libraries = new ArrayList<>(Suite.libraries);
	private boolean isTrace = false;

	public boolean processOption(String arg, Source<String> source) {
		return processOption(arg, source, true);
	}

	private boolean processOption(String arg, Source<String> source, boolean on) {
		var b = true;
		String arg1;

		if (Equals.string(arg, "--do"))
			isDo = on;
		else if (Equals.string(arg, "--chars"))
			isChars = on;
		else if (Equals.string(arg, "--eager"))
			isLazy = !on;
		else if (Equals.string(arg, "--editor"))
			EditorMain.main(null);
		else if (Equals.string(arg, "--imports") && (arg1 = source.g()) != null)
			imports = List.of(arg1.split(","));
		else if (Equals.string(arg, "--lazy"))
			isLazy = on;
		else if (Equals.string(arg, "--libraries") && (arg1 = source.g()) != null)
			libraries = List.of(arg1.split(","));
		else if (arg.startsWith("--no-"))
			b &= processOption("--" + arg.substring(5), source, false);
		else if (Equals.string(arg, "--quiet"))
			isQuiet = on;
		else if (Equals.string(arg, "--trace"))
			isTrace = on;
		else if (Equals.string(arg, "--use") && (arg1 = source.g()) != null)
			libraries = Concat.lists(libraries, List.of(arg1.split(",")));
		else
			fail("unknown option " + arg);

		return b;
	}

	public FunCompilerCfg fcc(Node node) {
		var pc = pc(Suite.newRuleSet());

		var fcc = new FunCompilerCfg(pc, libraries);
		fcc.setLazy(isLazy);
		fcc.setNode(node);

		return fcc;
	}

	public ProverCfg pc(RuleSet ruleSet) {
		var pc = new ProverCfg(ruleSet);
		pc.setTrace(isTrace);
		return pc;
	}

	public PrintStream prompt() {
		try (var os = new OutputStream() {
			public void write(int c) {
			}
		}) {
			return !isQuiet ? System.out : new PrintStream(os);
		} catch (IOException ex) {
			return fail(ex);
		}
	}

	public boolean isChars() {
		return isChars;
	}

	public boolean isDo() {
		return isDo;
	}

	public boolean isLazy() {
		return isLazy;
	}

	public boolean isQuiet() {
		return isQuiet;
	}

	public List<String> getImports() {
		return imports;
	}

}
