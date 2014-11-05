package suite.parser;

import java.io.IOException;
import java.io.StringWriter;

import suite.Suite;
import suite.node.Node;
import suite.node.io.Escaper;
import suite.util.FunUtil.Fun;

public class FunctionalTemplatePreprocessor {

	public String render(String template) {
		Fun<String, String> wrapText = s -> append(Escaper.escape(s, '"'));
		Fun<String, String> wrapExpression = this::append;
		String fps = new TemplatePreprocessor(wrapText, wrapExpression).apply(template);
		Node fp = Suite.parse("(" + fps + ")");

		try (StringWriter sw = new StringWriter()) {
			Suite.evaluateFunToWriter(Suite.fcc(fp), sw);
			return sw.toString();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private String append(String n) {
		return " | append {" + n + "}";
	}

}
