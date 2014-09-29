package suite.parser;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import suite.parser.Ebnf.Node;

public class EbnfDump {

	private String in;
	private Writer w = new StringWriter();

	public EbnfDump(Node node, String in) {
		this.in = in;
		try {
			prettyPrint(node, "");
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public String toString() {
		return w.toString();
	}

	private void prettyPrint(Node node, String indent) throws IOException {
		String indent1 = indent + "  ";
		int start = node.getStart();
		int end = node.getEnd();
		List<Node> nodes = node.getNodes();

		w.write(indent + node.getEntity() + "@" + start + "-" + end);

		if (nodes.isEmpty())
			w.write("[" + in.substring(start, end) + "]");

		w.write("\n");

		for (Node childNode : nodes)
			prettyPrint(childNode, indent1);
	}

}
