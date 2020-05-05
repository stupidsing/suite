package suite.funp.p0;

import static primal.statics.Rethrow.ex;

import java.io.IOException;

import primal.Verbs.ReadFile;
import primal.Verbs.ReadString;
import primal.fp.Funs.Fun;
import primal.io.ReadStream;
import primal.statics.Rethrow.FunIo;
import primal.statics.Rethrow.SourceEx;
import suite.Suite;
import suite.funp.Funp_;
import suite.funp.Funp_.Funp;
import suite.http.HttpClient;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.SwitchNode;
import suite.node.io.TermOp;

public class P00Consult {

	public Node c(Node node) {
		return new SwitchNode<Node>(node //
		).match("consult .0 ~ .1", (a, b) -> {
			return c(consult(Formatter.display(a).replace("${platform}", Funp_.isAmd64 ? "amd64" : "i686"), b));
		}).match("consult .0", a -> {
			return c(consult(Formatter.display(a)));
		}).applyIf(Tree.class, tree -> {
			return Tree.of(tree.getOperator(), c(tree.getLeft()), c(tree.getRight()));
		}).applyIf(Node.class, n -> {
			return node;
		}).nonNullResult();
	}

	private Node consult(String url) {
		FunIo<ReadStream, Node> r0 = is -> {
			var parsed = Suite.parse(ReadString.from(is));
			var predef = Tree.of(TermOp.ITEM__, Atom.of("predef"), Atom.of(url));
			return Tree.of(TermOp.TUPLE_, predef, parsed);
		};
		return consult_(url, is -> is.doRead(r0));
	}

	private Node consult(String url, Node program) {
		FunIo<ReadStream, Node> r0 = is -> {
			var node = Suite.parse(ReadString.from(is) + "$APP");
			return Tree //
					.read(node, TermOp.CONTD_) //
					.reverse() //
					.fold(program, (n, left) -> Tree.of(TermOp.CONTD_, left, n));
		};

		return consult_(url, is -> is.doRead(r0));
	}

	private Node consult_(String url, Fun<ReadStream, Node> r0) {
		Fun<SourceEx<ReadStream, IOException>, Node> r1 = source -> ex(source::g).doRead(r0::apply);

		if (url.startsWith("file://"))
			return r1.apply(() -> ReadFile.from(url.substring(7)));
		else if (url.startsWith("http://") || url.startsWith("https://"))
			return r0.apply(HttpClient.get(url).inputStream());
		else
			return r1.apply(() -> ReadStream.of(Funp.class.getResourceAsStream(url)));
	}

}
