package suite.asm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.Suite;
import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.CommentProcessor;
import suite.node.io.TermOp;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.util.FunUtil.Source;
import suite.util.Pair;
import suite.util.Util;

public class Assembler {

	private Prover prover = Suite.createProver(Arrays.asList("asm.sl", "auto.sl"));

	public Bytes assemble(String input) {
		Generalizer generalizer = new Generalizer();
		List<Pair<Node, Node>> lnis = new ArrayList<>();

		input = new CommentProcessor(Collections.singleton('\n')).apply(input);

		for (String line : input.split("\n")) {
			String l = null;

			if (line.startsWith(Generalizer.defaultPrefix)) {
				Pair<String, String> pair = Util.split2(line, " ");
				l = pair.t0.trim();
				line = pair.t1.trim();
			} else
				line = line.trim();

			Pair<String, String> pair = Util.split2(line, " ");
			Node insn = Atom.create(pair.t0);
			Node ps = convertOperands(Suite.parse(pair.t1));
			Node labelName = l != null ? Atom.create(l) : null;
			Node instruction = generalizer.generalize(Tree.create(TermOp.TUPLE_, insn, ps));
			lnis.add(Pair.create(labelName, instruction));
		}

		Map<Node, Node> addressesByLabel = new HashMap<>();
		BytesBuilder out = new BytesBuilder();

		for (boolean isPass2 : new boolean[] { false, true }) {
			out.clear();

			for (Pair<Node, Node> lni : lnis)
				if (lni.t0 != null) {
					Reference label = generalizer.getVariable(lni.t0);
					label.bound(!isPass2 ? Int.create(0) : addressesByLabel.get(lni.t0));
				}

			for (Pair<Node, Node> lni : lnis) {
				out.append(assemble(out.size(), lni.t1));

				if (!isPass2 && lni.t0 != null)
					addressesByLabel.put(lni.t0, Int.create(out.size()));
			}

			for (Pair<Node, Node> lni : lnis)
				if (lni.t0 != null)
					generalizer.getVariable(lni.t0).unbound();
		}

		return out.toBytes();
	}

	private Node convertOperands(Node node) {
		Tree tree = Tree.decompose(node, TermOp.AND___);
		if (tree != null)
			return Tree.create(TermOp.TUPLE_, tree.getLeft(), convertOperands(tree.getRight()));
		else
			return node;
	}

	private Bytes assemble(int address, Node instruction) {
		final Reference e = new Reference();
		final Bytes array[] = new Bytes[] { null };
		Node goal = Suite.substitute("as-insn .0 .1 .2/(), .3", Int.create(address), instruction, e, new Data<>(
				new Source<Boolean>() {
					public Boolean source() {
						array[0] = convertByteStream(e);
						return true;
					}
				}));
		// LogUtil.info(Formatter.dump(goal));

		prover.elaborate(goal);

		if (array[0] != null)
			return array[0];
		else
			throw new RuntimeException("Cannot assemble instruction " + instruction);
	}

	private Bytes convertByteStream(Node node) {
		BytesBuilder bb = new BytesBuilder();
		Tree tree;
		while ((tree = Tree.decompose(node, TermOp.AND___)) != null) {
			bb.append((byte) ((Int) tree.getLeft().finalNode()).getNumber());
			node = tree.getRight();
		}
		return bb.toBytes();
	}

}
