package org.instructionexecutor;

import java.util.ArrayList;
import java.util.List;

import org.instructionexecutor.InstructionUtil.Insn;
import org.instructionexecutor.InstructionUtil.Instruction;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Tree;

import com.google.common.collect.BiMap;

public class InstructionExtractor {

	private List<Instruction> enters = new ArrayList<>();
	private BiMap<Integer, Node> constantPool;

	public InstructionExtractor(BiMap<Integer, Node> constantPool) {
		this.constantPool = constantPool;
	}

	public List<Instruction> extractInstructions(Node node) {
		Tree tree;
		List<Instruction> list = new ArrayList<>();

		while ((tree = Tree.decompose(node, TermOp.AND___)) != null) {
			Instruction instruction = extract(tree.getLeft());
			list.add(instruction);
			node = tree.getRight();
		}

		return list;
	}

	private Instruction extract(Node node) {
		List<Node> rs = InstructionUtil.extractTuple(node);
		String insnName = ((Atom) rs.get(1).finalNode()).getName();
		Insn insn;

		switch (insnName) {
		case "ASSIGN-BOOL":
			insn = Insn.ASSIGNCONST___;
			break;
		case "ASSIGN-STR":
			insn = Insn.ASSIGNCONST___;
			break;
		case "EVALUATE":
			Atom atom = (Atom) rs.remove(4).finalNode();
			TermOp operator = TermOp.find(atom.getName());
			insn = InstructionUtil.getEvalInsn(operator);
			break;
		default:
			insn = InstructionUtil.getInsn(insnName);
		}

		if (insn != null) {
			Instruction instruction = new Instruction(insn //
					, getRegisterNumber(rs, 2) //
					, getRegisterNumber(rs, 3) //
					, getRegisterNumber(rs, 4));

			if (insn == Insn.ENTER_________)
				enters.add(instruction);
			else if (insn == Insn.LEAVE_________)
				enters.remove(enters.size() - 1);

			return instruction;
		} else
			throw new RuntimeException("Unknown opcode " + insnName);
	}

	private int getRegisterNumber(List<Node> rs, int index) {
		if (rs.size() > index) {
			Node node = rs.get(index).finalNode();

			if (node instanceof Int)
				return ((Int) node).getNumber();
			else if (node instanceof Reference) { // Transient register

				// Allocates new register in current local frame
				Instruction enter = enters.get(enters.size() - 1);
				int registerNumber = enter.op0++;

				((Reference) node).bound(Int.create(registerNumber));
				return registerNumber;
			} else
				// ASSIGN-BOOL, ASSIGN-STR, PROVE
				return allocateInPool(node);
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

}
