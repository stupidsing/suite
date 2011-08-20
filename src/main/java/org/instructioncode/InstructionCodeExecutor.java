package org.instructioncode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Str;
import org.suite.node.Tree;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class InstructionCodeExecutor {

	private enum Insn {
		ASSIGNBOOL____("ASSIGN-BOOL"), //
		ASSIGNFRAMEREG("ASSIGN-FRAME-REG"), //
		ASSIGNINT_____("ASSIGN-INT"), //
		ASSIGNSTR_____("ASSIGN-STR"), //
		ASSIGNCLOSURE_("ASSIGN-CLOSURE"), //
		CALLCLOSURE___("CALL-CLOSURE"), //
		ENTER_________("ENTER"), //
		EXIT__________("EXIT"), //
		EVALUATE______("EVALUATE"), //
		EVALADD_______("EVAL-ADD"), //
		IFFALSE_______("IF-FALSE"), //
		IFNOTEQUALS___("IF-NOT-EQ"), //
		JUMP__________("JUMP"), //
		LABEL_________("LABEL"), //
		PUSH__________("PUSH"), //
		POP___________("POP"), //
		RETURN________("RETURN"), //
		;

		private String name;

		private Insn(String name) {
			this.name = name;
		}
	};

	private final static BiMap<Insn, String> insnNames = HashBiMap.create();
	static {
		for (Insn insn : Insn.values())
			insnNames.put(insn, insn.name);
	}

	private Map<Integer, Object> objectPool = new HashMap<Integer, Object>();

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

		public String toString() {
			return insn.name + " " + op1 + ", " + op2 + ", " + op3;
		}
	}

	private Instruction instructions[];

	public InstructionCodeExecutor(Node node) {
		Tree tree;
		List<Instruction> list = new ArrayList<Instruction>();
		InstructionExtractor extractor = new InstructionExtractor();

		while ((tree = Tree.decompose(node, TermOp.AND___)) != null) {
			Instruction instruction = extractor.extract(tree.getLeft());
			list.add(instruction);
			node = tree.getRight();
		}

		instructions = list.toArray(new Instruction[list.size()]);
	}

	private class InstructionExtractor {
		private List<Instruction> enters = new ArrayList<Instruction>();

		private Instruction extract(Node node) {
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
				rs.set(3, rs.get(3) == trueAtom ? Int.create(1) : Int.create(0));
				break;
			case ASSIGNSTR_____:
				insn = Insn.ASSIGNINT_____;
				Str str = (Str) rs.get(3).finalNode();
				int pointer = allocate(str.getValue());
				rs.set(3, Int.create(pointer));
				break;
			case EVALUATE______:
				Atom atom = (Atom) rs.remove(4).finalNode();
				TermOp operator = TermOp.find((atom).getName());
				if (operator == TermOp.PLUS__)
					insn = Insn.EVALADD_______;
			}

			if (insn != null) {
				Instruction instruction = new Instruction(insn //
						, getRegisterNumber(rs, 2) //
						, getRegisterNumber(rs, 3) //
						, getRegisterNumber(rs, 4));

				if (insn == Insn.ENTER_________)
					enters.add(instruction);
				else if (insn == Insn.RETURN________)
					enters.remove(enters.size() - 1);

				return instruction;
			} else
				throw new RuntimeException("Unknown opcode"
						+ instNode.getName());
		}

		private int getRegisterNumber(List<Node> rs, int index) {
			if (rs.size() > index) {
				Node node = rs.get(index).finalNode();

				if (node instanceof Reference) {

					// Assigns new register in current local frame
					Instruction enter = enters.get(enters.size() - 1);
					int registerNumber = enter.op1++;

					((Reference) node).bound(Int.create(registerNumber));
					node = node.finalNode();
				}

				return ((Int) node).getNumber();
			} else
				return 0;
		}
	}

	private final static int STACKSIZE = 256;

	private static class Frame {
		private Frame previous;
		private int registers[];

		private Frame(Frame previous, int frameSize) {
			this.previous = previous;
			registers = new int[frameSize];
		}
	}

	private static class Closure {
		private Frame frame;
		private int ip;

		private Closure(Frame frame, int ip) {
			this.frame = frame;
			this.ip = ip;
		}
	}

	public int execute() {
		Closure current = new Closure(null, 0);
		Closure callStack[] = new Closure[STACKSIZE];
		int dataStack[] = new int[STACKSIZE];
		int csp = 0, dsp = 0;
		int counter = 0;

		for (;;) {
			Frame frame = current.frame;
			int registers[] = frame != null ? frame.registers : null;
			Instruction insn = instructions[current.ip++];

			switch (insn.insn) {
			case ASSIGNFRAMEREG:
				int i = insn.op2;
				while (i++ < 0)
					frame = frame.previous;
				registers[insn.op1] = frame.registers[insn.op3];
				break;
			case ASSIGNINT_____:
				registers[insn.op1] = insn.op2;
				break;
			case ASSIGNCLOSURE_:
				registers[insn.op1] = allocate(new Closure(frame, insn.op2));
				break;
			case CALLCLOSURE___:
				callStack[csp++] = current;
				current = (Closure) objectPool.get(registers[insn.op2]);
				break;
			case ENTER_________:
				current.frame = new Frame(frame, insn.op1);
				break;
			case EXIT__________:
				return registers[insn.op1];
			case EVALADD_______:
				registers[insn.op1] = registers[insn.op2] + registers[insn.op3];
				break;
			case IFFALSE_______:
				if (registers[insn.op2] != 1)
					current.ip = insn.op1;
				break;
			case IFNOTEQUALS___:
				if (registers[insn.op2] != registers[insn.op3])
					current.ip = insn.op1;
				break;
			case JUMP__________:
				current.ip = insn.op1;
				break;
			case PUSH__________:
				dataStack[dsp++] = registers[insn.op1];
				break;
			case POP___________:
				registers[insn.op1] = dataStack[--dsp];
				break;
			case RETURN________:
				int returnValue = registers[insn.op1]; // Saves return value
				current = callStack[--csp];
				current.frame.registers[instructions[current.ip - 1].op1] = returnValue;
			}

			if (++counter % 65536 == 0)
				collectGarbage(frame);
		}
	}

	/**
	 * Traverses frame hierarchy to find all referenced objects, and clean the
	 * unreferenced ones to free memory.
	 * 
	 * This does not free the holes in the reference list.
	 */
	private void collectGarbage(Frame frame) {
		int size = objectPool.size();
		Map<Integer, Object> objectPool1 = new HashMap<Integer, Object>(
				size * 2);

		collectGarbage0(objectPool1, frame);

		// Swaps object pool
		objectPool = objectPool1;
	}

	private void collectGarbage0(Map<Integer, Object> objectPool1, Frame frame) {
		if (frame != null) {
			for (int content : frame.registers) {
				Object object = objectPool.get(content);
				Object original = objectPool1.put(content, object);

				if (original == null && object instanceof Closure)
					collectGarbage0(objectPool1, ((Closure) object).frame);
			}

			collectGarbage0(objectPool1, frame.previous);
		}
	}

	private int allocate(Object object) {
		int pointer = objectPool.size();
		objectPool.put(pointer, object);
		return pointer;
	}

}
