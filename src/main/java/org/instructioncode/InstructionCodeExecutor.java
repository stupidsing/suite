package org.instructioncode;

import java.util.ArrayList;
import java.util.List;

import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Tree;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class InstructionCodeExecutor {

	private final static short ASSIGNBOOL____ = 0;
	private final static short ASSIGNFUNC____ = 1;
	private final static short ASSIGNFRAMEREG = 2;
	private final static short ASSIGNINT_____ = 3;
	private final static short ASSIGNSTR_____ = 4;
	private final static short ASSIGNLABEL___ = 5;
	private final static short CALL__________ = 6;
	private final static short EVALUATE______ = 7;
	private final static short EVALADD_______ = 8;
	private final static short IFFALSE_______ = 9;
	private final static short IFNOTEQUALS___ = 10;
	private final static short JUMP__________ = 11;
	private final static short LABEL_________ = 12;
	private final static short PUSH__________ = 13;
	private final static short POP___________ = 14;
	private final static short RETURN________ = 15;

	private final static BiMap<Short, String> insnNames = HashBiMap.create();
	static {
		insnNames.put(ASSIGNBOOL____, "ASSIGN-BOOL");
		insnNames.put(ASSIGNFUNC____, "ASSIGN-FUNC");
		insnNames.put(ASSIGNFRAMEREG, "ASSIGN-FRAME-REG");
		insnNames.put(ASSIGNINT_____, "ASSIGN-INT");
		insnNames.put(ASSIGNSTR_____, "ASSIGN-STR");
		insnNames.put(ASSIGNLABEL___, "ASSIGN-LABEL");
		insnNames.put(CALL__________, "CALL");
		insnNames.put(EVALUATE______, "EVALUATE");
		insnNames.put(EVALADD_______, "EVAL-ADD");
		insnNames.put(IFFALSE_______, "IF-FALSE");
		insnNames.put(IFNOTEQUALS___, "IF-NOT-EQ");
		insnNames.put(JUMP__________, "JUMP");
		insnNames.put(LABEL_________, "LABEL");
		insnNames.put(PUSH__________, "PUSH");
		insnNames.put(POP___________, "POP");
		insnNames.put(RETURN________, "RETURN");
	}

	private static class Instruction {
		private short instruction;
		private short op1, op2, op3;

		public Instruction(short instruction, short op1, short op2, short op3) {
			this.instruction = instruction;
			this.op1 = op1;
			this.op2 = op2;
			this.op3 = op3;
		}
	}

	private Instruction instructions[];

	public InstructionCodeExecutor(Node node) {
		Tree tree;
		List<Instruction> list = new ArrayList<Instruction>();

		while ((tree = Tree.decompose(node, TermOp.SEP___)) != null) {
			list.add(parseInstruction(tree.getLeft()));
			node = tree.getRight();
		}

		instructions = list.toArray(new Instruction[list.size()]);
	}

	private Instruction parseInstruction(Node node) {
		List<Node> rs = new ArrayList<Node>(5);
		Tree tree;

		while ((tree = Tree.decompose(node, TermOp.SEP___)) != null) {
			rs.add(tree.getLeft());
			node = tree.getRight();
		}

		rs.add(node);

		Atom instNode = (Atom) rs.get(0);
		Short instruction = insnNames.inverse().get(instNode.getName());

		if (instruction == EVALUATE______) {
			TermOp operator = TermOp.find(((Atom) rs.get(3)).getName());
			if (operator == TermOp.PLUS__)
				instruction = EVALADD_______;

			rs.remove(3);
		}

		return new Instruction(instruction //
				, (short) (rs.size() > 0 ? ((Int) rs.get(0)).getNumber() : 0) //
				, (short) (rs.size() > 1 ? ((Int) rs.get(1)).getNumber() : 0) //
				, (short) (rs.size() > 2 ? ((Int) rs.get(2)).getNumber() : 0));
	}

	public void execute() {
		int ip = 0;
		int registers[] = new int[256];

		Instruction inst = instructions[ip];

		switch (inst.instruction) {
		case ASSIGNINT_____:
			registers[inst.op1] = inst.op2;
			break;
		case EVALADD_______:
			registers[inst.op1] = registers[inst.op2] + registers[inst.op3];
			break;
		case IFNOTEQUALS___:
			if (registers[inst.op1] != registers[inst.op2])
				ip = inst.op1;
			break;
		case JUMP__________:
			ip = inst.op1;
		}
	}

}
