package suite.instructionexecutor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import suite.instructionexecutor.InstructionUtil.Insn;
import suite.instructionexecutor.InstructionUtil.Instruction;
import suite.lp.Journal;
import suite.lp.doer.Binder;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.TermParser.TermOp;

import com.google.common.collect.BiMap;

public class InstructionExtractor implements AutoCloseable {

	private Deque<Instruction> enters = new ArrayDeque<>();
	private BiMap<Integer, Node> constantPool;
	private Journal journal = new Journal();

	public InstructionExtractor(BiMap<Integer, Node> constantPool) {
		this.constantPool = constantPool;
	}

	public List<Instruction> extractInstructions(Node node) {
		List<Instruction> list = new ArrayList<>();
		for (Node elem : Node.iter(node))
			list.add(extract(elem));
		return list;
	}

	private Instruction extract(Node node) {
		List<Node> rs = Node.tupleToList(node);
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

			if (node instanceof Int)
				return ((Int) node).getNumber();
			else if (node instanceof Reference) { // Transient register

				// Allocates new register in current local frame
				Instruction enter = enters.getFirst();
				int registerNumber = enter.op0++;

				Binder.bind(node, Int.create(registerNumber), journal);
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

	@Override
	public void close() {
		journal.undoAllBinds();
	}

}
