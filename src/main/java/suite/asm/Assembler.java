package suite.asm;

import static primal.statics.Fail.fail;
import static primal.statics.Fail.failBool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import primal.MoreVerbs.Read;
import primal.MoreVerbs.Split;
import primal.Nouns.Utf8;
import primal.Verbs.Is;
import primal.Verbs.Right;
import primal.Verbs.Take;
import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.primitive.adt.Bytes;
import primal.primitive.adt.Bytes.BytesBuilder;
import suite.Suite;
import suite.asm.Assembler.Asm;
import suite.assembler.Amd64Assemble;
import suite.assembler.Amd64Mode;
import suite.assembler.Amd64Parse;
import suite.lp.doer.Binder;
import suite.lp.doer.Generalizer;
import suite.lp.kb.RuleSet;
import suite.lp.search.ProverBuilder.Finder;
import suite.lp.search.SewingProverBuilder2;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.SwitchNode;
import suite.node.io.TermOp;
import suite.parser.CommentPreprocessor;
import suite.text.Preprocess;
import suite.text.Preprocess.Run;

public class Assembler {

	private Asm asm;
	private Fun<List<Pair<Reference, Node>>, List<Pair<Reference, Node>>> preassemble;

	public interface Asm {
		public Bytes assemble(boolean isPass2, int address, Node instruction);
	}

	public Assembler(Amd64Mode mode) {
		this(mode, lnis -> lnis);
	}

	public Assembler(Amd64Mode mode, Fun<List<Pair<Reference, Node>>, List<Pair<Reference, Node>>> preassemble) {
		asm = Boolean.TRUE ? new AsmA(mode) : new AsmSl(mode.addrSize * 8, mode == Amd64Mode.LONG64);
		this.preassemble = preassemble;
	}

	public Bytes assemble(String in0) {
		var whitespaces = Collections.singleton('\n');
		Fun<String, List<Run>> gct = CommentPreprocessor.ofGroupComment(whitespaces)::preprocess;
		Fun<String, List<Run>> lct = CommentPreprocessor.ofLineComment(whitespaces)::preprocess;
		var in1 = Preprocess.transform(List.of(gct, lct), in0).k;

		var generalizer = new Generalizer();
		var lines = List.of(in1.split("\n"));
		Pair<String, String> pe;
		var start = 0;

		while ((pe = Split.string(lines.get(start), "=")) != null) {
			generalizer.getVariable(Atom.of(pe.k)).bound(Suite.parse(pe.v));
			start++;
		}

		var lnis = Read //
				.from(Right.of(lines, start)) //
				.map(line -> Split.strl(line, "\t").map((label, command) -> {
					var reference = Is.notBlank(label) //
							? generalizer.getVariable(Atom.of(label)) //
							: new Reference();
					var instruction = generalizer.generalize(Suite.parse(command));
					return Pair.of(reference, instruction);
				})).toList();

		return assemble(generalizer, lnis);
	}

	public Bytes assemble(Node input) {
		var lnis = new ArrayList<Pair<Reference, Node>>();
		var generalizer = new Generalizer();

		for (var node : Tree.read(input))
			new SwitchNode<Boolean>(generalizer.generalize(node) //
			).match(".0 = .1", (l, r) -> {
				return Binder.bind(l, r) || failBool("bind failed");
			}).match(".0 .1", (l, r) -> {
				return lnis.add(Pair.of((Reference) l, r));
			}).nonNullResult();

		return assemble(generalizer, preassemble.apply(lnis));
	}

	private Bytes assemble(Generalizer generalizer, List<Pair<Reference, Node>> lnis) {
		var org = Int.num(generalizer.getVariable(Atom.of(".org")).finalNode());
		var out = new BytesBuilder();

		for (var isPass2 : new boolean[] { false, true, }) {
			AssemblePredicates.isPass2 = isPass2;
			out.clear();

			for (var lni : lnis) {
				Bytes bytes = lni.map((reference, instruction) -> {
					var address = org + out.size();

					if (!isPass2)
						reference.bound(Int.of(address));
					else if (Int.num(reference.finalNode()) != address)
						fail("address varied between passes at " + Integer.toHexString(address) + ": " + instruction);

					try {
						return asm.assemble(isPass2, address, instruction);
					} catch (Exception ex) {
						return fail("in " + instruction + " during pass " + (!isPass2 ? "1" : "2"), ex);
					}
				});
				out.append(bytes);
			}

			if (isPass2)
				for (var lni : lnis)
					lni.k.unbound();
		}

		return out.toBytes();
	}

}

class AsmA implements Asm {

	private Amd64Assemble aa;
	private Amd64Parse ap;

	public AsmA(Amd64Mode mode) {
		aa = new Amd64Assemble(mode);
		ap = new Amd64Parse(mode, TermOp.TUPLE_);
	}

	public Bytes assemble(boolean isPass2, int address, Node instruction) {
		if (instruction == Atom.NIL)
			return Bytes.empty;
		else if (instruction instanceof Str)
			return Bytes.of(Str.str(instruction).getBytes(Utf8.charset));
		else
			return aa.assemble(isPass2, address, ap.parse(instruction));
	}

}

class AsmSl implements Asm {

	private RuleSet ruleSet;
	private Finder finder;
	private int bits;

	public AsmSl(int bits, boolean isLongMode) {
		ruleSet = Suite.newRuleSet(List.of("asm.sl", "auto.sl"));

		if (isLongMode)
			Suite.addRule(ruleSet, "as-long-mode");

		finder = new SewingProverBuilder2() //
				.build(ruleSet) //
				.apply(Suite.parse("" //
						+ "source (.bits, .address, .instruction,)" //
						+ ", asi:.bits:.address .instruction .code" //
						+ ", sink .code"));

		this.bits = bits;
	}

	public Bytes assemble(boolean isPass2, int address, Node instruction) {
		var ins = Suite.substitute(".0, .1, .2,", Int.of(bits), Int.of(address), instruction);
		var bytesList = new ArrayList<Bytes>();
		finder.find(Take.from(ins), node -> bytesList.add(convertByteStream(node)));
		return Read.from(bytesList).min((bytes0, bytes1) -> bytes0.size() - bytes1.size());

	}

	private Bytes convertByteStream(Node node) {
		var bb = new BytesBuilder();
		for (var n : Tree.read(node))
			bb.append((byte) Int.num(n));
		return bb.toBytes();
	}

}
