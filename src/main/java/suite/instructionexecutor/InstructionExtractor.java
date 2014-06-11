package suite.instructionexecutor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

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

import com.google.common.collect.BiMap;

public class InstructionExtractor implements AutoCloseable {

	private Deque<Instruction> enters = new ArrayDeque<>();
	private BiMap<Integer, Node> constantPool;
	private Journal journal = new Journal();

	public InstructionExtractor(BiMap<Integer, Node> constantPool) {
		this.constantPool = constantPool;
	}

	public void extractInstructions(List<Instruction> list, Node node) {
		int ip = list.size(); // Assigns instruction pointer
		for (Node elem : Tree.iter(node))
			Binder.bind(Tree.decompose(elem).getLeft(), Int.of(ip++), journal);

		for (Node elem : Tree.iter(node))
			list.add(extract(elem));
	}

	private Instruction extract(Node node) {
		List<Node> rs = tupleToList(node);
		String insnName = ((Atom) rs.get(1).finalNode()).getName();
		Insn insn;

		if (Objects.equals(insnName, "EVALUATE")) {
			Atom atom = (Atom) rs.remove(4).finalNode();
			TermOp operator = TermOp.find(atom.getName());
			insn = InstructionUtil.getEvalInsn(operator);
		} else
			insn = InstructionUtil.getInsn(insnName);

		if (insn != null) {
			Instruction instruction;
			instruction = new Instruction(insn //
					, getRegisterNumber(rs, 2) //
					, getRegisterNumber(rs, 3) //
					, getRegisterNumber(rs, 4));

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

				switch (((Atom) tree.getLeft()).getName()) {
				case "c":
					return allocateInPool(n0);
				case "i":
					return ((Int) n0).getNumber();
				case "l":
					Node n1 = Tree.decompose(n0, TermOp.AND___).getLeft();
					Node n2 = Tree.decompose(n1, TermOp.TUPLE_).getLeft();
					return ((Int) n2.finalNode()).getNumber();
				case "r":
					return 0;
				}
			}

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
