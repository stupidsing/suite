package suite.instructionexecutor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import suite.instructionexecutor.InstructionUtil.Insn;
import suite.instructionexecutor.InstructionUtil.Instruction;
import suite.lp.Journal;
import suite.lp.doer.Binder;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.util.Util;

import com.google.common.collect.BiMap;

public class InstructionExtractor implements AutoCloseable {

	private Deque<Instruction> enters = new ArrayDeque<>();
	private BiMap<Integer, Node> constantPool;
	private Journal journal = new Journal();

	private static final Atom KEYC = Atom.of("c");
	private static final Atom KEYL = Atom.of("l");
	private static final Atom KEYR = Atom.of("r");
	private static final Atom PROC = Atom.of("PROC");

	public InstructionExtractor(BiMap<Integer, Node> constantPool) {
		this.constantPool = constantPool;
	}

	public List<Instruction> extractInstructions(Node node) {
		List<List<Node>> insnNodes = new ArrayList<>();
		extractInstructions(node, insnNodes);
		return insnNodes.stream().map(this::extract).collect(Collectors.toList());
	}

	private void extractInstructions(Node node, List<List<Node>> insnNodes) {
		Deque<Node> deque = new ArrayDeque<>();
		deque.add(node);

		while (!deque.isEmpty()) {
			Tree tree = Tree.decompose(deque.pop(), TermOp.AND___);
			Tree tree1;

			if (tree != null) {
				List<Node> rs = tupleToList(tree.getLeft());
				Node label = rs.get(0).finalNode();

				if (label instanceof Reference) {
					Binder.bind(label, Int.of(insnNodes.size()), journal);

					if (rs.get(1) == PROC) {
						insnNodes.add(Arrays.asList(Atom.of("ENTER")));
						extractInstructions(rs.get(2), insnNodes);
						insnNodes.add(Arrays.asList(Atom.of("LEAVE")));
					} else {
						insnNodes.add(Util.right(rs, 1));
						for (Node op : Util.right(rs, 2))
							if ((tree1 = Tree.decompose(op, TermOp.COLON_)) != null && tree1.getLeft() == KEYL)
								deque.push(tree1.getRight());
						deque.push(tree.getRight());
					}
				} else
					insnNodes.add(Arrays.asList(Atom.of("JUMP"), label));
			}
		}
	}

	private Instruction extract(List<Node> rs) {
		String insnName = ((Atom) rs.get(0).finalNode()).getName();
		Insn insn;

		if (Objects.equals(insnName, "EVALUATE")) {
			Atom atom = (Atom) rs.remove(3).finalNode();
			TermOp operator = TermOp.find(atom.getName());
			insn = InstructionUtil.getEvalInsn(operator);
		} else
			insn = InstructionUtil.getInsn(insnName);

		if (insn != null) {
			Instruction instruction;
			instruction = new Instruction(insn //
					, getRegisterNumber(rs, 1) //
					, getRegisterNumber(rs, 2) //
					, getRegisterNumber(rs, 3));

			if (insn == Insn.ENTER_________)
				enters.push(instruction);
			else if (insn == Insn.LEAVE_________)
				enters.pop();

			return instruction;
		} else
			throw new RuntimeException("Unknown opcode " + insnName);
	}

	private List<Node> tupleToList(Node node) {
		List<Node> results = new ArrayList<>();
		Tree tree;
		while ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null) {
			results.add(tree.getLeft());
			node = tree.getRight();
		}
		results.add(node);
		return results;
	}

	private int getRegisterNumber(List<Node> rs, int index) {
		if (rs.size() > index) {
			Node node = rs.get(index).finalNode();
			Tree tree;

			if (node instanceof Int)
				return ((Int) node).getNumber();
			else if (node instanceof Reference) { // Transient register

				// Allocates new register in current local frame
				Instruction enter = enters.getFirst();
				int registerNumber = enter.op0++;

				Binder.bind(node, Int.of(registerNumber), journal);
				return registerNumber;
			} else if ((tree = Tree.decompose(node, TermOp.COLON_)) != null) {
				Node n0 = tree.getRight().finalNode();
				Node left = tree.getLeft();

				if (left == KEYC)
					return allocateInPool(n0);
				else if (left == KEYL) {
					Node n1 = Tree.decompose(n0, TermOp.AND___).getLeft();
					Node n2 = Tree.decompose(n1, TermOp.TUPLE_).getLeft();
					return ((Int) n2.finalNode()).getNumber();
				} else if (left == KEYR)
					return 0;
			} else
				return allocateInPool(node);

			throw new RuntimeException("Cannot parse instruction " + rs + " operand " + index);
		} else
			return 0;
	}

	private int allocateInPool(Node node) {
		Integer pointer = constantPool.inverse().get(node);

		if (pointer == null) {
			int pointer1 = constantPool.size();
			constantPool.put(pointer1, node);
			return pointer1;
		} else
			return pointer;
	}

	@Override
	public void close() {
		journal.undoAllBinds();
	}

}
