package suite.asm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import suite.Suite;
import suite.adt.Pair;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.lp.doer.Generalizer;
import suite.lp.kb.RuleSet;
import suite.lp.search.ProverBuilder.Finder;
import suite.lp.search.SewingProverBuilder2;
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
import suite.text.Preprocess;
import suite.text.Preprocess.Run;
import suite.util.FunUtil.Fun;
import suite.util.String_;
import suite.util.To;
import suite.util.Util;

public class Assembler {

	private RuleSet ruleSet;
	private Finder finder;
	private int bits;
	private Fun<List<Pair<Reference, Node>>, List<Pair<Reference, Node>>> preassemble;

	public Assembler(int bits) {
		this(bits, false);
	}

	public Assembler(int bits, boolean isLongMode) {
		this(bits, isLongMode, lnis -> lnis);
	}

	public Assembler(int bits, boolean isLongMode, Fun<List<Pair<Reference, Node>>, List<Pair<Reference, Node>>> preassemble) {
		ruleSet = Suite.newRuleSet(Arrays.asList("asm.sl", "auto.sl"));

		if (isLongMode)
			Suite.addRule(ruleSet, "as-long-mode");

		finder = new SewingProverBuilder2().build(ruleSet)
				.apply(Suite.parse("" //
						+ "source (.bits, .address, .instruction,)" //
						+ ", asi:.bits:.address .instruction .code" //
						+ ", sink .code" //
		));

		this.bits = bits;
		this.preassemble = preassemble;
	}

	public Bytes assemble(String in0) {
		Set<Character> whitespaces = Collections.singleton('\n');
		Fun<String, List<Run>> gct = CommentPreprocessor.groupCommentPreprocessor(whitespaces);
		Fun<String, List<Run>> lct = CommentPreprocessor.lineCommentPreprocessor(whitespaces);
		String in1 = Preprocess.transform(Arrays.asList(gct, lct), in0).t0;

		Generalizer generalizer = new Generalizer();
		List<String> lines = Arrays.asList(in1.split("\n"));
		Pair<String, String> pe;
		int start = 0;

		while (!(pe = String_.split2(lines.get(start), "=")).t1.isEmpty()) {
			generalizer.getVariable(Atom.of(pe.t0)).bound(Suite.parse(pe.t1));
			start++;
		}

		List<Pair<Reference, Node>> lnis = Read.from(Util.right(lines, start)) //
				.map(line -> {
					Pair<String, String> pt = String_.split2(line, "\t");
					String label = pt.t0;
					String command = pt.t1;

					Reference reference = String_.isNotBlank(label) ? generalizer.getVariable(Atom.of(label)) : null;
					Node instruction = generalizer.generalize(Suite.parse(command));
					return Pair.of(reference, instruction);
				}).toList();

		return assemble(generalizer, lnis);
	}

	public Bytes assemble(Node input) {
		Generalizer generalizer = new Generalizer();
		Trail trail = new Trail();
		List<Pair<Reference, Node>> lnis = new ArrayList<>();

		for (Node node0 : Tree.iter(input)) {
			Node node = generalizer.generalize(node0);
			Tree tree;

			if ((tree = Tree.decompose(node, TermOp.EQUAL_)) != null)
				if (!Binder.bind(tree.getLeft(), tree.getRight(), trail))
					throw new RuntimeException("Bind failed");
				else
					;
			else if ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null)
				lnis.add(Pair.of((Reference) tree.getLeft(), tree.getRight()));
			else
				throw new RuntimeException("Cannot assemble " + node);
		}

		return assemble(generalizer, preassemble.apply(lnis));
	}

	private Bytes assemble(Generalizer generalizer, List<Pair<Reference, Node>> lnis) {
		int org = ((Int) generalizer.getVariable(Atom.of(".org")).finalNode()).number;
		BytesBuilder out = new BytesBuilder();

		for (boolean isPass2 : new boolean[] { false, true, }) {
			AssemblePredicates.isPass2 = isPass2;
			out.clear();

			for (Pair<Reference, Node> lni : lnis) {
				int address = org + out.size();

				if (lni.t0 != null)
					if (!isPass2)
						lni.t0.bound(Int.of(address));
					else if (((Int) lni.t0.finalNode()).number != address)
						throw new RuntimeException("Address varied between passes at " + Integer.toHexString(address));

				out.append(assemble(isPass2, address, lni.t1));
			}

			for (Pair<Reference, Node> lni : lnis)
				if (lni.t0 != null && isPass2)
					lni.t0.unbound();
		}

		return out.toBytes();
	}

	private Bytes assemble(boolean isPass2, int address, Node instruction) {
		try {
			List<Node> ins = Arrays.asList(Int.of(bits), Int.of(address), instruction);
			List<Bytes> bytesList = new ArrayList<>();
			finder.find(To.source(Tree.of(TermOp.AND___, ins)), node -> bytesList.add(convertByteStream(node)));
			return Read.from(bytesList).min((bytes0, bytes1) -> bytes0.size() - bytes1.size());
		} catch (Exception ex) {
			throw new RuntimeException("In " + instruction + " during pass " + (!isPass2 ? "1" : "2"), ex);
		}
	}

	private Bytes convertByteStream(Node node) {
		BytesBuilder bb = new BytesBuilder();
		Tree tree;
		while ((tree = Tree.decompose(node, TermOp.AND___)) != null) {
			bb.append((byte) ((Int) tree.getLeft()).number);
			node = tree.getRight();
		}
		return bb.toBytes();
	}

}
