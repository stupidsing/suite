package suite.ebnf;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import suite.ebnf.Ebnf.Node;
import suite.util.Util;

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
		String entity0 = node.entity;
		List<Node> nodes;

		while ((nodes = node.nodes).size() == 1)
			node = nodes.get(0);

		if (nodes.size() != 1) {
			String indent1 = indent + "  ";
			String entity1 = node.entity;
			int start = node.getStart();
			int end = node.getEnd();

			w.write(indent + entity0);
			if (!Util.stringEquals(entity0, entity1))
				w.write(".." + entity1);
			w.write("@" + start + "-" + end);
			if (nodes.isEmpty())
				w.write("[" + in.substring(start, end) + "]");
			w.write("\n");

			for (Node childNode : nodes)
				prettyPrint(childNode, indent1);
		} else
			prettyPrint(nodes.get(0), indent);
	}

}
