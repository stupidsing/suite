package suite.asm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
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
import suite.node.io.TermOp;
import suite.parser.CommentPreprocessor;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.util.FunUtil.Source;
import suite.util.Pair;
import suite.util.Util;

public class Assembler {

	private Prover prover = Suite.createProver(Arrays.asList("asm.sl", "auto.sl"));

	private int bits;

	public Assembler(int bits) {
		this.bits = bits;
	}

	public Bytes assemble(String input) {
		CommentPreprocessor commentPreprocessor = new CommentPreprocessor(Collections.singleton('\n'));
		Generalizer generalizer = new Generalizer();
		List<String> lines = Arrays.asList(commentPreprocessor.apply(input).split("\n"));
		Pair<String, String> pe;
		int start = 0;

		while (!(pe = Util.split2(lines.get(start), "=")).t1.isEmpty()) {
			generalizer.getVariable(Atom.create(pe.t0)).bound(Suite.parse(pe.t1));
			start++;
		}

		int org = ((Int) generalizer.getVariable(Atom.create(".org")).finalNode()).getNumber();

		List<Pair<Reference, Node>> lnis = new ArrayList<>();

		for (String line : Util.right(lines, start)) {
			Pair<String, String> pt = Util.split2(line, "\t");
			String label = pt.t0;
			String operation = pt.t1;

			Pair<String, String> pi = Util.split2(operation, " ");
			Atom insn = Atom.create(pi.t0);
			Node operands = Suite.parse(pi.t1);

			Reference reference = Util.isNotBlank(label) ? generalizer.getVariable(Atom.create(label)) : null;
			Node instruction = generalizer.generalize(Tree.create(TermOp.TUPLE_, insn, operands));
			lnis.add(Pair.create(reference, instruction));
		}

		return assemble(org, generalizer, lnis);
	}

	private Bytes assemble(int org, Generalizer generalizer, List<Pair<Reference, Node>> lnis) {
		Map<Reference, Node> addressesByLabel = new IdentityHashMap<>();
		BytesBuilder out = new BytesBuilder();

		for (boolean isPass2 : new boolean[] { false, true }) {
			out.clear();

			for (Pair<Reference, Node> lni : lnis)
				if (lni.t0 != null && isPass2)
					lni.t0.bound(addressesByLabel.get(lni.t0));

			for (Pair<Reference, Node> lni : lnis) {
				try {
					out.append(assemble(org + out.size(), lni.t1));
				} catch (Exception ex) {
					throw new RuntimeException("In " + lni.t1, ex);
				}

				if (!isPass2 && lni.t0 != null)
					addressesByLabel.put(lni.t0, Int.create(org + out.size()));
			}

			for (Pair<Reference, Node> lni : lnis)
				if (lni.t0 != null && isPass2)
					lni.t0.unbound();
		}

		return out.toBytes();
	}

	private Bytes assemble(int address, Node instruction) {
		final Reference e = new Reference();
		final List<Bytes> list = new ArrayList<>();

		Node goal = Suite.substitute("asi:.0 (.1 .2) .3, .4", Int.create(bits), Int.create(address), instruction, e, new Data<>(
				new Source<Boolean>() {
					public Boolean source() {
						list.add(convertByteStream(e));
						return true;
					}
				}));
		// LogUtil.info(Formatter.dump(goal));

		prover.elaborate(goal);

		if (!list.isEmpty())
			return Collections.min(list, new Comparator<Bytes>() {
				public int compare(Bytes bytes0, Bytes bytes1) {
					return bytes0.size() - bytes1.size();
				}
			});
		else
			throw new RuntimeException("Failure");
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
