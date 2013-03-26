package org.instructionexecutor;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.instructionexecutor.InstructionUtil.Closure;
import org.instructionexecutor.InstructionUtil.Frame;
import org.instructionexecutor.InstructionUtil.Instruction;
import org.instructionexecutor.io.IndexedIo.IndexedInput;
import org.instructionexecutor.io.IndexedIo.IndexedInputStream;
import org.instructionexecutor.io.IndexedIo.IndexedOutput;
import org.instructionexecutor.io.IndexedIo.IndexedOutputStream;
import org.suite.SuiteUtil;
import org.suite.doer.Comparer;
import org.suite.doer.Generalizer;
import org.suite.doer.Prover;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Tree;
import org.suite.node.Vector;

public class FunctionInstructionExecutor extends InstructionExecutor {

	private static final Atom COMPARE = Atom.create("COMPARE");
	private static final Atom CONS = Atom.create("CONS");
	private static final Atom FFLUSH = Atom.create("FFLUSH");
	private static final Atom FGETC = Atom.create("FGETC");
	private static final Atom FPUTC = Atom.create("FPUTC");
	private static final Atom HEAD = Atom.create("HEAD");
	private static final Atom ISTREE = Atom.create("IS-TREE");
	private static final Atom ISVECTOR = Atom.create("IS-VECTOR");
	private static final Atom LOG = Atom.create("LOG");
	private static final Atom LOG2 = Atom.create("LOG2");
	private static final Atom PROVE = Atom.create("PROVE");
	private static final Atom SUBST = Atom.create("SUBST");
	private static final Atom TAIL = Atom.create("TAIL");
	private static final Atom VCONCAT = Atom.create("VCONCAT");
	private static final Atom VCONS = Atom.create("VCONS");
	private static final Atom VELEM = Atom.create("VELEM");
	private static final Atom VEMPTY = Atom.create("VEMPTY");
	private static final Atom VHEAD = Atom.create("VHEAD");
	private static final Atom VRANGE = Atom.create("VRANGE");
	private static final Atom VTAIL = Atom.create("VTAIL");

	private Comparer comparer = new Comparer();
	private Prover prover;
	private Map<Node, IndexedInput> inputs = new HashMap<>();
	private Map<Node, IndexedOutput> outputs = new HashMap<>();

	public FunctionInstructionExecutor(Node node) {
		super(node);
	}

	@Override
	protected int[] execute(Closure current, Instruction insn,
			Closure callStack[], int csp, Object dataStack[], int dsp) {
		Frame frame = current.frame;
		Object regs[] = frame != null ? frame.registers : null;

		switch (insn.insn) {
		case SERVICE_______:
			dsp -= insn.op3;
			regs[insn.op1] = sys(constantPool.get(insn.op2), dataStack, dsp);
			break;
		default:
			return super.execute(current, insn, callStack, csp, dataStack, dsp);
		}

		return new int[] { csp, dsp };
	}

	private Node sys(Node command, Object dataStack[], int dsp) {
		Node result;

		if (command == COMPARE) {
			Node left = (Node) dataStack[dsp + 1];
			Node right = (Node) dataStack[dsp];
			result = Int.create(comparer.compare(left, right));
		} else if (command == CONS) {
			Node left = (Node) dataStack[dsp + 1];
			Node right = (Node) dataStack[dsp];
			result = Tree.create(TermOp.AND___, left, right);
		} else if (command == FFLUSH) {
			Node node = (Node) dataStack[dsp];
			try {
				outputs.get(node).close();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
			result = (Node) dataStack[dsp];
		} else if (command == FGETC) {
			Node node = (Node) dataStack[dsp + 1];
			int p = ((Int) dataStack[dsp]).getNumber();
			result = Int.create(inputs.get(node).read(p));
		} else if (command == FPUTC) {
			Node node = (Node) dataStack[dsp + 3];
			int p = ((Int) dataStack[dsp + 2]).getNumber();
			int c = ((Int) dataStack[dsp + 1]).getNumber();
			outputs.get(node).write(p, c);
			result = (Node) dataStack[dsp];
		} else if (command == HEAD)
			result = Tree.decompose((Node) dataStack[dsp]).getLeft();
		else if (command == ISTREE)
			result = a(Tree.decompose((Node) dataStack[dsp]) != null);
		else if (command == ISVECTOR)
			result = a(dataStack[dsp] instanceof Vector);
		else if (command == LOG)
			System.err.println(result = (Node) dataStack[dsp]);
		else if (command == LOG2) {
			System.err.println((Node) dataStack[dsp + 1]);
			result = (Node) dataStack[dsp];
		} else if (command == PROVE) {
			if (prover == null)
				prover = SuiteUtil.getProver(new String[] { "auto.sl" });

			Node node = (Node) dataStack[dsp];
			Tree tree = Tree.decompose(node, TermOp.JOIN__);
			if (tree != null)
				if (prover.prove(tree.getLeft()))
					result = tree.getRight().finalNode();
				else
					throw new RuntimeException("Goal failed");
			else
				result = prover.prove(node) ? Atom.TRUE : Atom.FALSE;
		} else if (command == SUBST) {
			Generalizer g = new Generalizer();
			g.setVariablePrefix("_");

			Node var = (Node) dataStack[dsp + 1];
			Tree tree = (Tree) g.generalize((Node) dataStack[dsp]);
			((Reference) tree.getRight()).bound(var);
			result = tree.getLeft();
		} else if (command == TAIL)
			result = Tree.decompose((Node) dataStack[dsp]).getRight();
		else if (command == VCONCAT) {
			Vector left = (Vector) dataStack[dsp + 1];
			Vector right = (Vector) dataStack[dsp];
			result = Vector.concat(left, right);
		} else if (command == VCONS) {
			Node head = (Node) dataStack[dsp + 1];
			Vector tail = (Vector) dataStack[dsp];
			result = Vector.cons(head, tail);
		} else if (command == VELEM)
			result = new Vector((Node) dataStack[dsp]);
		else if (command == VEMPTY)
			result = Vector.EMPTY;
		else if (command == VHEAD)
			result = ((Vector) dataStack[dsp]).get(0);
		else if (command == VRANGE) {
			Vector vector = (Vector) dataStack[dsp + 2];
			int s = ((Int) dataStack[dsp + 1]).getNumber();
			int e = ((Int) dataStack[dsp]).getNumber();
			return vector.subVector(s, e);
		} else if (command == VTAIL)
			result = ((Vector) dataStack[dsp]).subVector(1, 0);
		else
			throw new RuntimeException("Unknown system call " + command);

		return result;
	}

	public void setIn(InputStream in) {
		inputs.put(Atom.NIL, new IndexedInputStream(in));
	}

	public void setOut(PrintStream out) {
		outputs.put(Atom.NIL, new IndexedOutputStream(out));
	}

	public void setProver(Prover prover) {
		this.prover = prover;
	}

}
