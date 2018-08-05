package suite.assembler;

import static suite.util.Friends.fail;

import java.util.ArrayList;

import suite.Suite;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.Operand;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public class Amd64Parse {

	private static Amd64 amd64 = Amd64.me;

	public Instruction parse(Node node) {
		var tree = Tree.decompose(node, TermOp.TUPLE_);
		var insn = Enum.valueOf(Insn.class, Atom.name(tree.getLeft()));
		var ops = tree.getRight();
		var operands = scan(ops, ".0, .1").map(this::parseOperand).toList();

		return amd64.instruction(insn, //
				0 < operands.size() ? operands.get(0) : amd64.none, //
				1 < operands.size() ? operands.get(1) : amd64.none, //
				2 < operands.size() ? operands.get(2) : amd64.none);
	}

	public Operand parseOperand(Node node) {
		Operand operand;
		Node[] m;

		if ((operand = amd64.registerByName.get(node)) != null)
			return operand;
		else if ((m = Suite.pattern("BYTE `.0`").match(node)) != null)
			return parseOpMem(m, 1);
		else if ((m = Suite.pattern("WORD `.0`").match(node)) != null)
			return parseOpMem(m, 2);
		else if ((m = Suite.pattern("DWORD `.0`").match(node)) != null)
			return parseOpMem(m, 4);
		else if ((m = Suite.pattern("`.0`").match(node)) != null)
			return parseOpMem(m, 4);
		else if (node instanceof Int) {
			var opImm = amd64.new OpImm();
			opImm.imm = Int.num(node);
			opImm.size = 4;
			return opImm;
		} else
			return fail("bad operand");
	}

	private Operand parseOpMem(Node[] m, int size) {
		var opDisp = amd64.new OpImm();
		opDisp.size = 0;

		var opMem = amd64.new OpMem();
		opMem.size = size;
		opMem.indexReg = -1;
		opMem.baseReg = -1;
		opMem.disp = opDisp;

		for (var component : scan(m[0], ".0 + .1"))
			if ((m = Suite.pattern(".0 * .1").match(component)) != null)
				if (opMem.indexReg < 0) {
					opMem.indexReg = amd64.regByName.get(m[0]).reg;
					opMem.scale = Int.num(m[1]);
				} else
					fail("bad operand");
			else if (component instanceof Int)
				if (opMem.disp.size == 0) {
					opMem.disp.imm = Int.num(component);
					opMem.disp.size = 4;
				} else
					fail("bad operand");
			else if (opMem.baseReg < 0)
				opMem.baseReg = amd64.regByName.get(component).reg;
			else
				fail("bad operand");
		return opMem;
	}

	private Streamlet<Node> scan(Node ops, String pattern) {
		var nodes = new ArrayList<Node>();
		Node[] m;
		while ((m = Suite.pattern(pattern).match(ops)) != null) {
			nodes.add(m[0]);
			ops = m[1];
		}
		if (ops != Atom.NIL)
			nodes.add(ops);
		return Read.from(nodes);
	}

}
