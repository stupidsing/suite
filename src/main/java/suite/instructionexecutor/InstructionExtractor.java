package suite.instructionexecutor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import suite.node.util.IdHashKey;
import suite.util.Util;

import com.google.common.collect.BiMap;

public class InstructionExtractor implements AutoCloseable {

	private Map<IdHashKey, Integer> ipsByLabelId = new HashMap<>();
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

	@Override
	public void close() {
		journal.undoAllBinds();
	}

	public List<Instruction> extractInstructions(Node node) {
		List<List<Node>> rsList = new ArrayList<>();
		extractInstructions(node, rsList);
		return rsList.stream().map(this::extract).collect(Collectors.toList());
	}

	private void extractInstructions(Node snippet, List<List<Node>> rsList) {
		Deque<Node> deque = new ArrayDeque<>();
		deque.add(snippet);
		Tree tree, tree1;

		while (!deque.isEmpty()) {
			if ((tree = Tree.decompose(deque.pop(), TermOp.AND___)) != null) {
				IdHashKey key = new IdHashKey(tree);
				Integer ip = ipsByLabelId.get(key);

				if (ip == null) {
					ipsByLabelId.put(key, ip = rsList.size());
					List<Node> rs = tupleToList(tree.getLeft());

					if (rs.get(0) == PROC) {
						rsList.add(Arrays.asList(Atom.of("ENTER")));
						extractInstructions(rs.get(1), rsList);
						rsList.add(Arrays.asList(Atom.of("LEAVE")));
					} else {
						rsList.add(rs);
						for (Node op : Util.right(rs, 1))
							if ((tree1 = Tree.decompose(op, TermOp.COLON_)) != null && tree1.getLeft() == KEYL)
								deque.push(tree1.getRight());
						deque.push(tree.getRight());
					}
				} else
					rsList.add(Arrays.asList(Atom.of("JUMP"), Int.of(ip)));
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
			Instruction instruction = new Instruction(insn //
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
				Node key = tree.getLeft(), value = tree.getRight().finalNode();

				if (key == KEYC)
					return allocateInPool(value);
				else if (key == KEYL)
					return ipsByLabelId.get(new IdHashKey(value));
				else if (key == KEYR)
					return 0;
			}

			throw new RuntimeException("Cannot parse instruction " + rs.get(0) + " operand " + node);
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

}
