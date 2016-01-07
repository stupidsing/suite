package suite.parser;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import suite.Suite;
import suite.node.Atom;
import suite.node.Node;
import suite.node.io.Escaper;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;

public class FunctionalTemplateRenderer {

	public String render(String template, Map<String, Node> inputs) {
		Fun<String, String> wrapText = s -> " . append {" + Escaper.escape(s, '"') + "}";
		Fun<String, String> wrapExpression = s -> !s.startsWith("=") ? s : " . append {" + s.substring(1) + "}";

		String fps = "id " + new TemplateRenderer(wrapText, wrapExpression).apply(template);
		Node fp0 = Suite.substitute("() | .0", Suite.parse(fps));
		Node fp1 = Read.from2(inputs).pairs().fold(fp0,
				(fp_, p) -> Suite.substitute("let .1 := .2 >> .0", fp_, Atom.of(p.t0), p.t1));
		Node fp2 = Suite.applyWriter(fp1);

		try (StringWriter sw = new StringWriter()) {
			Suite.evaluateFunToWriter(Suite.fcc(fp2), sw);
			return sw.toString();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
