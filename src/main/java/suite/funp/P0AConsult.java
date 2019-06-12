package suite.funp;

import static suite.util.Friends.rethrow;

import java.io.IOException;

import suite.Suite;
import suite.http.HttpUtil;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.SwitchNode;
import suite.node.io.TermOp;
import suite.os.FileUtil;
import suite.streamlet.FunUtil.Fun;
import suite.util.ReadStream;
import suite.util.Rethrow.FunIo;
import suite.util.Rethrow.SourceEx;

public class P0AConsult {

	public Node c(Node node) {
		return new SwitchNode<Node>(node //
		).match("consult .0 ~ .1", (a, b) -> {
			return c(consult(Str.str(a).replace("${platform}", Funp_.isAmd64 ? "amd64" : "i686"), b));
		}).match("consult .0", a -> {
			return c(consult(Str.str(a)));
		}).applyIf(Node.class, n -> {
			var tree = Tree.decompose(node);
			return tree != null ? Tree.of(tree.getOperator(), c(tree.getLeft()), c(tree.getRight())) : node;
		}).nonNullResult();
	}

	private Node consult(String url) {
		FunIo<ReadStream, Node> r0 = is -> {
			var parsed = Suite.parse(FileUtil.read(is));
			var predef = Tree.of(TermOp.ITEM__, Atom.of("predef"), Atom.of(url));
			return Tree.of(TermOp.TUPLE_, predef, parsed);
		};
		return consult_(url, is -> is.doRead(r0));
	}

	private Node consult(String url, Node program) {
		FunIo<ReadStream, Node> r0 = is -> {
			var node = Suite.parse(FileUtil.read(is) + "$APP");
			return Tree //
					.iter(node, TermOp.CONTD_) //
					.reverse() //
					.fold(program, (n, left) -> Tree.of(TermOp.CONTD_, left, n));
		};

		return consult_(url, is -> is.doRead(r0));
	}

	private Node consult_(String url, Fun<ReadStream, Node> r0) {
		Fun<SourceEx<ReadStream, IOException>, Node> r1 = source -> rethrow(source::g).doRead(r0::apply);

		if (url.startsWith("file://"))
			return r1.apply(() -> FileUtil.in(url.substring(7)));
		else if (url.startsWith("http://") || url.startsWith("https://"))
			return r0.apply(HttpUtil.get(url).inputStream());
		else
			return r1.apply(() -> ReadStream.of(getClass().getResourceAsStream(url)));
	}

}
