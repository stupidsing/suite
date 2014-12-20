package suite.asm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import suite.Suite;
import suite.lp.Journal;
import suite.lp.doer.Binder;
import suite.lp.doer.Generalizer;
import suite.lp.kb.RuleSet;
import suite.lp.search.FindUtil;
import suite.lp.search.ProverBuilder.Finder;
import suite.lp.search.SewingProverBuilder;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.parser.CommentPreprocessor;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.streamlet.Read;
import suite.util.Pair;
import suite.util.Util;

public class Assembler {

	private int bits;

	private RuleSet ruleSet = Suite.createRuleSet(Arrays.asList("asm.sl", "auto.sl"));

	private Finder finder = new SewingProverBuilder().build(ruleSet).apply(Suite.parse("" //
			+ "source (.bits, .address, .instruction,)" //
			+ ", asi:.bits:.address .instruction .code" //
			+ ", sink .code" //
	));

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
			generalizer.getVariable(Atom.of(pe.t0)).bound(Suite.parse(pe.t1));
			start++;
		}

		List<Pair<Reference, Node>> lnis = Read.from(Util.right(lines, start)) //
				.map(line -> {
					Pair<String, String> pt = Util.split2(line, "\t");
					String label = pt.t0;
					String command = pt.t1;

					Reference reference = Util.isNotBlank(label) ? generalizer.getVariable(Atom.of(label)) : null;
					Node instruction = generalizer.generalize(Suite.parse(command));
					return Pair.of(reference, instruction);
				}).toList();

		return assemble(generalizer, lnis);
	}

	public Bytes assemble(Node input) {
		Generalizer generalizer = new Generalizer();
		Journal journal = new Journal();
		List<Pair<Reference, Node>> lnis = new ArrayList<>();

		for (Node node0 : Tree.iter(input)) {
			Node node = generalizer.generalize(node0);
			Tree tree;

			if ((tree = Tree.decompose(node, TermOp.EQUAL_)) != null)
				Binder.bind(tree.getLeft(), tree.getRight(), journal);
			else if ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null)
				lnis.add(Pair.of((Reference) tree.getLeft(), tree.getRight()));
			else
				throw new RuntimeException("Cannot assemble " + node);
		}

		return assemble(generalizer, preassemble(lnis));
	}

	public List<Pair<Reference, Node>> preassemble(List<Pair<Reference, Node>> lnis) {
		return lnis;
	}

	private Bytes assemble(Generalizer generalizer, List<Pair<Reference, Node>> lnis) {
		int org = ((Int) generalizer.getVariable(Atom.of(".org")).finalNode()).number;
		BytesBuilder out = new BytesBuilder();

		for (boolean isPass2 : new boolean[] { false, true }) {
			out.clear();

			for (Pair<Reference, Node> lni : lnis) {
				int address = org + out.size();

				try {
					out.append(assemble(address, lni.t1));
				} catch (Exception ex) {
					throw new RuntimeException("In " + lni.t1, ex);
				}

				if (lni.t0 != null)
					if (!isPass2)
						lni.t0.bound(Int.of(address));
					else if (((Int) lni.t0.finalNode()).number != address)
						throw new RuntimeException("Address varied between passes at " + Integer.toHexString(address));
			}

			for (Pair<Reference, Node> lni : lnis)
				if (lni.t0 != null && isPass2)
					lni.t0.unbound();
		}

		return out.toBytes();
	}

	private Bytes assemble(int address, Node instruction) {
		List<Node> ins = Arrays.asList(Int.of(bits), Int.of(address), instruction);
		List<Node> nodes = FindUtil.collectList(finder, Tree.of(TermOp.AND___, ins));
		return Read.from(nodes) //
				.map(this::convertByteStream) //
				.min((bytes0, bytes1) -> bytes0.size() - bytes1.size());
	}

	private Bytes convertByteStream(Node node) {
		BytesBuilder bb = new BytesBuilder();
		Tree tree;
		while ((tree = Tree.decompose(node, TermOp.AND___)) != null) {
			bb.append((byte) ((Int) tree.getLeft().finalNode()).number);
			node = tree.getRight();
		}
		return bb.toBytes();
	}

}
