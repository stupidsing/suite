package org.instructioncode;

import java.util.ArrayList;
import java.util.List;

import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Str;
import org.suite.node.Tree;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class InstructionCodeExecutor {

	private enum Insn {
		ASSIGNBOOL____, //
		ASSIGNFRAMEREG, //
		ASSIGNINT_____, //
		ASSIGNSTR_____, //
		ASSIGNLABEL___, //
		CALL__________, //
		EVALUATE______, //
		EVALADD_______, //
		IFFALSE_______, //
		IFNOTEQUALS___, //
		JUMP__________, //
		LABEL_________, //
		PUSH__________, //
		POP___________, //
		RETURN________, //
	};

	private final static BiMap<Insn, String> insnNames = HashBiMap.create();
	static {
		insnNames.put(Insn.ASSIGNBOOL____, "ASSIGN-BOOL");
		insnNames.put(Insn.ASSIGNFRAMEREG, "ASSIGN-FRAME-REG");
		insnNames.put(Insn.ASSIGNINT_____, "ASSIGN-INT");
		insnNames.put(Insn.ASSIGNSTR_____, "ASSIGN-STR");
		insnNames.put(Insn.ASSIGNLABEL___, "ASSIGN-LABEL");
		insnNames.put(Insn.CALL__________, "CALL");
		insnNames.put(Insn.EVALUATE______, "EVALUATE");
		insnNames.put(Insn.EVALADD_______, "EVAL-ADD");
		insnNames.put(Insn.IFFALSE_______, "IF-FALSE");
		insnNames.put(Insn.IFNOTEQUALS___, "IF-NOT-EQ");
		insnNames.put(Insn.JUMP__________, "JUMP");
		insnNames.put(Insn.LABEL_________, "LABEL");
		insnNames.put(Insn.PUSH__________, "PUSH");
		insnNames.put(Insn.POP___________, "POP");
		insnNames.put(Insn.RETURN________, "RETURN");
	}

	private List<String> stringLiterals = new ArrayList<String>();

	private final static Atom trueAtom = Atom.create("true");

	private static class Instruction {
		private Insn insn;
		private int op1, op2, op3;

		public Instruction(Insn insn, int op1, int op2, int op3) {
			this.insn = insn;
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
			Instruction instruction = parseInstruction(tree.getLeft());
			if (instruction != null) // Do not add pseudo-instructions
				list.add(instruction);
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

		Atom instNode = (Atom) rs.get(1);
		Insn insn = insnNames.inverse().get(instNode.getName());

		switch (insn) {
		case ASSIGNBOOL____:
			insn = Insn.ASSIGNINT_____;
			rs.set(2, rs.get(2) == trueAtom ? Int.create(1) : Int.create(0));
			break;
		case ASSIGNLABEL___:
			insn = Insn.ASSIGNINT_____;
			break;
		case ASSIGNSTR_____:
			insn = Insn.ASSIGNINT_____;
			rs.set(2, Int.create(stringLiterals.size()));
			stringLiterals.add(((Str) rs.get(2).finalNode()).getValue());
			break;
		case EVALUATE______:
			Atom atom = (Atom) rs.remove(3);
			TermOp operator = TermOp.find((atom).getName());
			if (operator == TermOp.PLUS__)
				insn = Insn.EVALADD_______;
		}

		if (insn != null) {
			int size = rs.size();
			int op1 = size > 2 ? ((Int) rs.get(2).finalNode()).getNumber() : 0;
			int op2 = size > 3 ? ((Int) rs.get(3).finalNode()).getNumber() : 0;
			int op3 = size > 4 ? ((Int) rs.get(4).finalNode()).getNumber() : 0;
			return new Instruction(insn, op1, op2, op3);
		} else
			return null;
	}

	public void execute() {
		int magicSize = 256;

		int frames[][] = new int[magicSize][];
		int registers[] = new int[magicSize];
		int callStack[] = new int[magicSize];
		int dataStack[] = new int[magicSize];
		int ip = 0, csp = 0, dsp = 0;

		for (;;) {
			Instruction insn = instructions[ip++];

			switch (insn.insn) {
			case ASSIGNFRAMEREG:
				registers[insn.op1] = frames[csp + insn.op2][insn.op3];
				break;
			case ASSIGNINT_____:
				registers[insn.op1] = insn.op2;
				break;
			case ASSIGNLABEL___:
				registers[insn.op1] = insn.op2;
				break;
			case CALL__________:
				frames[csp] = registers;
				callStack[csp++] = ip;
				registers = new int[magicSize];
				break;
			case EVALADD_______:
				registers[insn.op1] = registers[insn.op2] + registers[insn.op3];
				break;
			case IFFALSE_______:
				if (registers[insn.op1] != 1)
					ip = insn.op1;
				break;
			case IFNOTEQUALS___:
				if (registers[insn.op1] != registers[insn.op2])
					ip = insn.op1;
				break;
			case JUMP__________:
				ip = insn.op1;
				break;
			case PUSH__________:
				dataStack[dsp++] = registers[insn.op1];
				break;
			case POP___________:
				registers[insn.op1] = dataStack[--dsp];
				break;
			case RETURN________:
				registers = frames[--csp];
				ip = callStack[csp];
				frames[csp] = null;
			}
		}
	}

}
