package suite.parser;

import primal.MoreVerbs.Read;
import primal.fp.Funs.Iterate;
import suite.Suite;
import suite.node.Atom;
import suite.node.Node;
import suite.node.io.Escaper;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import static primal.statics.Fail.fail;

public class RenderFunctionalTemplate {

	public String render(String template, Map<String, Node> inputs) {
		Iterate<String> wrapText = s -> " . append_{" + Escaper.escape(s, '"') + "}";
		Iterate<String> wrapExpression = s -> !s.startsWith("=") ? s : " . append_{" + s.substring(1) + "}";

		var fps = "id " + new TemplateRenderer(wrapText, wrapExpression).apply(template);
		var fp0 = Suite.substitute("() | .0", Suite.parse(fps));
		var fp1 = Read.from2(inputs).pairs().fold(fp0, (fp_, p) -> Suite.substitute("let .1 := .2 ~ .0", fp_, Atom.of(p.k), p.v));
		var fp2 = Suite.applyWriter(fp1);

		try (var sw = new StringWriter()) {
			Suite.evaluateFunToWriter(Suite.fcc(fp2), sw);
			return sw.toString();
		} catch (IOException ex) {
			return fail(ex);
		}
	}

}
