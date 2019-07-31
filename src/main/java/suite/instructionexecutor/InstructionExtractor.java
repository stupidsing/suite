package suite.instructionexecutor;

import static primal.statics.Fail.fail;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import primal.Ob;
import suite.adt.IdentityKey;
import suite.adt.map.BiMap;
import suite.instructionexecutor.InstructionUtil.Insn;
import suite.instructionexecutor.InstructionUtil.Instruction;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.streamlet.Read;
import suite.util.List_;

public class InstructionExtractor implements AutoCloseable {

	private Map<IdentityKey<Node>, Integer> ipByLabelId = new HashMap<>();
	private Deque<Instruction> frameBegins = new ArrayDeque<>();
	private BiMap<Integer, Node> constantPool;
	private Trail trail = new Trail();

	private static Atom KEYC = Atom.of("c");
	private static Atom KEYL = Atom.of("l");
	private static Atom KEYR = Atom.of("r");
	private static Atom FRAME = Atom.of("FRAME");

	public InstructionExtractor(BiMap<Integer, Node> constantPool) {
		this.constantPool = constantPool;
	}

	@Override
	public void close() {
		trail.unwindAll();
	}

	public List<Instruction> extractInstructions(Node node) {
		var rsList = new ArrayList<List<Node>>();
		extractInstructions(node, rsList);
		return Read.from(rsList).map(this::extract).toList();
	}

	private void extractInstructions(Node snippet, List<List<Node>> rsList) {
		var deque = new ArrayDeque<Node>();
		deque.add(snippet);
		Tree tree;
		Node value;

		while (!deque.isEmpty())
			if ((tree = Tree.decompose(deque.pop(), TermOp.AND___)) != null) {
				IdentityKey<Node> key = IdentityKey.of(tree);
				var ip = ipByLabelId.get(key);

				if (ip == null) {
					ipByLabelId.put(key, ip = rsList.size());
					var rs = tupleToList(tree.getLeft());

					if (rs.get(0) == FRAME)
						if ((value = label(rs.get(1))) != null) {
							rsList.add(List.of(Atom.of("FRAME-BEGIN")));
							extractInstructions(value, rsList);
							rsList.add(List.of(Atom.of("FRAME-END")));
						} else
							fail("bad frame definition");
					else {
						rsList.add(rs);
						for (var op : List_.right(rs, 1))
							if ((value = label(op)) != null)
								deque.push(value);
						deque.push(tree.getRight());
					}
				} else
					rsList.add(List.of(Atom.of("JUMP"), Int.of(ip)));
			}
	}

	private Instruction extract(List<Node> rs) {
		var insnName = Atom.name(rs.get(0));
		Insn insn;

		if (Ob.equals(insnName, "EVALUATE")) {
			var operator = TermOp.find(Atom.name(rs.remove(3)));
			insn = InstructionUtil.getEvalInsn(operator);
		} else
			insn = InstructionUtil.getInsn(insnName);

		if (insn != null) {
			var instruction = new Instruction(insn //
					, getRegisterNumber(rs, 1) //
					, getRegisterNumber(rs, 2) //
					, getRegisterNumber(rs, 3));

			if (insn == Insn.FRAMEBEGIN____)
				frameBegins.push(instruction);
			else if (insn == Insn.FRAMEEND______)
				frameBegins.pop();

			return instruction;
		} else
			return fail("unknown opcode " + insnName);
	}

	private int getRegisterNumber(List<Node> rs, int index) {
		if (index < rs.size()) {
			var node = rs.get(index).finalNode();
			Tree tree;

			if (node instanceof Int)
				return Int.num(node);
			else if (node instanceof Reference) { // transient register

				// allocates new register in current local frame
				var frameBegin = frameBegins.getFirst();
				var registerNumber = frameBegin.op0++;

				Binder.bind(node, Int.of(registerNumber), trail);
				return registerNumber;
			} else if ((tree = Tree.decompose(node, TermOp.COLON_)) != null) {
				Node key = tree.getLeft(), value = tree.getRight();

				if (key == KEYC)
					return allocateInPool(value);
				else if (key == KEYL)
					return ipByLabelId.get(IdentityKey.of(value));
				else if (key == KEYR)
					return 0;
			}

			return fail("cannot parse instruction " + rs.get(0) + " operand " + node);
		} else
			return 0;
	}

	private Node label(Node node) {
		Tree tree1;
		if ((tree1 = Tree.decompose(node, TermOp.COLON_)) != null && tree1.getLeft() == KEYL)
			return tree1.getRight();
		else
			return null;
	}

	private int allocateInPool(Node node) {
		var pointer = constantPool.inverse().get(node);

		if (pointer == null) {
			var pointer1 = constantPool.size();
			constantPool.put(pointer1, node);
			return pointer1;
		} else
			return pointer;
	}

	private List<Node> tupleToList(Node node) {
		var results = new ArrayList<Node>();
		Tree tree;
		while ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null) {
			results.add(tree.getLeft());
			node = tree.getRight();
		}
		results.add(node);
		return results;
	}

}
