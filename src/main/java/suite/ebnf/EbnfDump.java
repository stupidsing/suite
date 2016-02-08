package suite.ebnf;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import suite.ebnf.Ebnf.Ast;
import suite.util.Util;

public class EbnfDump {

	private String in;
	private Writer w = new StringWriter();

	public EbnfDump(Ast ast, String in) {
		this.in = in;
		try {
			prettyPrint(ast, "");
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public String toString() {
		return w.toString();
	}

	private void prettyPrint(Ast ast, String indent) throws IOException {
		String entity0 = ast.entity;
		List<Ast> children;

		while ((children = ast.children).size() == 1)
			ast = children.get(0);

		if (children.size() != 1) {
			String indent1 = indent + "  ";
			String entity1 = ast.entity;
			int start = ast.getStart();
			int end = ast.getEnd();

			w.write(indent + entity0);
			if (!Util.stringEquals(entity0, entity1))
				w.write(".." + entity1);
			w.write("@" + start + "-" + end);
			if (children.isEmpty())
				w.write("[" + in.substring(start, end) + "]");
			w.write("\n");

			for (Ast child : children)
				prettyPrint(child, indent1);
		} else
			prettyPrint(children.get(0), indent);
	}

}
