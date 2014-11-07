package suite.parser;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Map.Entry;

import suite.Suite;
import suite.node.Atom;
import suite.node.Node;
import suite.node.io.Escaper;
import suite.util.FunUtil.Fun;

public class FunctionalTemplatePreprocessor {

	public String render(String template, Map<String, Node> inputs) {
		Fun<String, String> wrapText = s -> " . append {" + Escaper.escape(s, '"') + "}";
		Fun<String, String> wrapExpression = s -> !s.startsWith("=") ? s : " . append {" + s.substring(1) + "}";

		String fps = "id " + new TemplatePreprocessor(wrapText, wrapExpression).apply(template);

		Node fp = Suite.substitute("() | .0", Suite.parse(fps));
		for (Entry<String, Node> e : inputs.entrySet())
			fp = Suite.substitute("let .1 := .2 >> .0", fp, Atom.of(e.getKey()), e.getValue());
		fp = Suite.applyWriter(fp);

		try (StringWriter sw = new StringWriter()) {
			Suite.evaluateFunToWriter(Suite.fcc(fp), sw);
			return sw.toString();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
