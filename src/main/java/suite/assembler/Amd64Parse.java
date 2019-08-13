package suite.assembler;

import static primal.statics.Fail.fail;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

import primal.MoreVerbs.Read;
import primal.streamlet.Streamlet;
import suite.Suite;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.Operand;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.TermOp;

public class Amd64Parse {

	private static Amd64 amd64 = Amd64.me;
	private Map<Node, Operand> references = new IdentityHashMap<>();

	public Instruction parse(Node node) {
		var tree = Tree.decompose(node, TermOp.TUPLE_);
		var insn = Insn.valueOf(Atom.name(tree.getLeft()));
		var ops = tree.getRight();
		var operands = scan(ops, ".0, .1").map(this::parseOperand).toList();

		return amd64.instruction(insn, //
				0 < operands.size() ? operands.get(0) : amd64.none, //
				1 < operands.size() ? operands.get(1) : amd64.none, //
				2 < operands.size() ? operands.get(2) : amd64.none);
	}

	public Operand parseOperand(Node node_) {
		Operand operand;
		Node[] m;
		var node = node_.finalNode();

		if ((operand = parseOperand(node_, 4)) != null)
			return operand;
		else if (node instanceof Atom && (operand = amd64.registerByName.get(node)) != null)
			return operand;
		else if ((m = Suite.pattern("BYTE .0").match(node)) != null)
			return parseOperandCast(m, 1);
		else if ((m = Suite.pattern("WORD .0").match(node)) != null)
			return parseOperandCast(m, 2);
		else if ((m = Suite.pattern("DWORD .0").match(node)) != null)
			return parseOperandCast(m, 4);
		else if ((m = Suite.pattern("QWORD .0").match(node)) != null)
			return parseOperandCast(m, 8);
		else if ((m = Suite.pattern("`.0`").match(node)) != null)
			return parseOpMem(m, 4);
		else
			return fail("bad operand");
	}

	private Operand parseOperandCast(Node[] m0, int size) {
		var m1 = Suite.pattern("`.0`").match(m0[0]);
		return m1 != null ? parseOpMem(m1, size) : parseOperand(m0[0], size);
	}

	private Operand parseOperand(Node node, int size) {
		if (node instanceof Int)
			return amd64.imm(Int.num(node), size);
		else if (node instanceof Reference)
			return references.computeIfAbsent(node, n -> amd64.new OpImmLabel(size));
		else
			return null;
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
			else if (component instanceof Int) {
				opMem.disp.imm = Int.num(component);
				opMem.disp.size = opMem.disp.size == 0 ? 4 : fail("bad operand");
			} else if (component instanceof Reference)
				opMem.disp.size = opMem.disp.size == 0 ? 4 : fail("bad operand");
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
