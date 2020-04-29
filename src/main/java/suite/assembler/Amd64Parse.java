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
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.SwitchNode;
import suite.node.io.TermOp;

public class Amd64Parse {

	private static Amd64 amd64 = Amd64.me;

	private Amd64Mode mode;
	private Map<Node, Operand> references = new IdentityHashMap<>();

	public Amd64Parse(Amd64Mode mode) {
		this.mode = mode;
	}

	public Instruction parse(Node node) {
		var tree = Tree.decompose(node, TermOp.TUPLE_);
		var insn = Insn.valueOf(Atom.name(tree.getLeft()));
		var ops = tree.getRight();
		var operands = scan(ops, ".0, .1").map(this::parseOperand).toList();

		return amd64.instruction(insn,
				0 < operands.size() ? operands.get(0) : amd64.none,
				1 < operands.size() ? operands.get(1) : amd64.none,
				2 < operands.size() ? operands.get(2) : amd64.none);
	}

	public Operand parseOperand(Node node_) {
		Operand operand;
		var node = node_.finalNode();

		if ((operand = parseOperand(node_, 4)) != null)
			return operand;
		else if (node instanceof Atom && (operand = amd64.registerByName.get(node)) != null)
			return operand;
		else
			return new SwitchNode<Operand>(node)
					.match("BYTE .0", n -> parseOperandCast(n, 1))
					.match("WORD .0", n -> parseOperandCast(n, 2))
					.match("DWORD .0", n -> parseOperandCast(n, 4))
					.match("QWORD .0", n -> parseOperandCast(n, 8))
					.match("`.0`", n -> parseOpMem(n, 4))
					.nonNullResult();
	}

	private Operand parseOperandCast(Node node, int size) {
		var m1 = Suite.pattern("`.0`").match(node);
		return m1 != null ? parseOpMem(m1[0], size) : parseOperand(node, size);
	}

	private Operand parseOperand(Node node, int size) {
		if (node instanceof Int)
			return amd64.imm(Int.num(node), size);
		else if (node instanceof Reference)
			return references.computeIfAbsent(node, n -> amd64.new OpImmLabel(size));
		else
			return null;
	}

	private Operand parseOpMem(Node node, int size) {
		var opDisp = amd64.new OpImm();
		opDisp.size = 0;

		var opMem = amd64.new OpMem();
		opMem.size = size;
		opMem.indexReg = -1;
		opMem.baseReg = -1;
		opMem.disp = opDisp;

		Node[] m;
		OpReg reg;

		for (var component : scan(node, ".0 + .1"))
			if ((m = Suite.pattern(".0 * .1").match(component)) != null)
				if (opMem.indexReg < 0) {
					opMem.indexReg = amd64.regByName.get(m[0]).reg;
					opMem.scale = Int.num(m[1]);
				} else
					fail("bad operand");
			else if (component instanceof Int) {
				opDisp.imm = Int.num(component);
				opDisp.size = opDisp.size == 0 ? mode.dispSize : fail("bad operand");
			} else if (component instanceof Reference)
				opDisp.size = opDisp.size == 0 ? mode.dispSize : fail("bad operand");
			else if ((reg = amd64.regByName.get(component)).size == mode.addrSize)
				if (mode.addrSize == 2)
					if (reg.reg == amd64.bxReg || reg.reg == amd64.bpReg)
						opMem.baseReg = opMem.baseReg < 0 ? reg.reg : fail();
					else if (reg.reg == amd64.siReg || reg.reg == amd64.diReg)
						opMem.indexReg = opMem.indexReg < 0 ? reg.reg : fail();
					else
						fail("bad operand");
				else if (opMem.baseReg < 0)
					opMem.baseReg = reg.reg;
				else if (opMem.indexReg < 0) {
					opMem.indexReg = reg.reg;
					opMem.scale = 1;
				} else
					fail("bad operand");
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
