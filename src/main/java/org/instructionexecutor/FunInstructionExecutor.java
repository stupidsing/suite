package org.instructionexecutor;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.instructionexecutor.InstructionUtil.Activation;
import org.instructionexecutor.InstructionUtil.Closure;
import org.instructionexecutor.InstructionUtil.Frame;
import org.instructionexecutor.InstructionUtil.Instruction;
import org.instructionexecutor.io.IndexedIo.IndexedInput;
import org.instructionexecutor.io.IndexedIo.IndexedReader;
import org.suite.Suite;
import org.suite.doer.Comparer;
import org.suite.doer.Formatter;
import org.suite.doer.Generalizer;
import org.suite.doer.Prover;
import org.suite.doer.ProverConfig;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Tree;
import org.suite.node.Vector;
import org.util.LogUtil;

public class FunInstructionExecutor extends InstructionExecutor {

	private Comparer comparer = new Comparer();
	private ProverConfig proverConfig;
	private Map<Node, IndexedInput> inputs = new HashMap<>();

	public FunInstructionExecutor(Node node) {
		super(node);
	}

	/**
	 * Evaluates the whole (lazy) term to a list of numbers, and converts to a
	 * string.
	 */
	public String unwrapToString(Node node) {
		StringWriter writer = new StringWriter();

		try {
			unwrap(node, writer);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		return writer.toString();
	}

	/**
	 * Evaluates the whole (lazy) term to a list of numbers, and write
	 * corresponding characters into the writer.
	 */
	public void unwrap(Node node, Writer writer) throws IOException {
		while (true) {
			node = node.finalNode();

			if (node instanceof Tree) {
				Tree tree = (Tree) node;
				writer.write(((Int) unwrap(tree.getLeft())).getNumber());
				node = tree.getRight();
			} else if (node instanceof Closure)
				node = evaluateClosure((Closure) node);
			else if (node == Atom.NIL)
				break;
		}
	}

	/**
	 * Evaluates the whole (lazy) term to actual by invoking all the thunks.
	 */
	public Node unwrap(Node node) {
		node = node.finalNode();

		if (node instanceof Tree) {
			Tree tree = (Tree) node;
			Node left = unwrap(tree.getLeft());
			Node right = unwrap(tree.getRight());
			node = Tree.create(tree.getOperator(), left, right);
		} else if (node instanceof Closure)
			node = unwrap(evaluateClosure((Closure) node));

		return node;
	}

	public void executeIo(Reader reader, Writer writer) throws IOException {
		inputs.put(Atom.NIL, new IndexedReader(reader));
		unwrap(super.execute(), writer);
	}

	@Override
	protected void handle(Exec exec, Instruction insn) {
		Activation current = exec.current;
		Frame frame = current.frame;
		Object regs[] = frame != null ? frame.registers : null;

		Object stack[] = exec.stack;
		int sp = exec.sp;

		Node node, left, right, result;
		Tree tree;

		switch (insn.insn) {
		case COMPARE_______:
			left = (Node) stack[--sp];
			right = (Node) stack[--sp];
			result = Int.create(comparer.compare(left, right));
			break;
		case CONS__________:
			left = (Node) stack[--sp];
			right = (Node) stack[--sp];
			result = Tree.create(TermOp.AND___, left, right);
			break;
		case ERROR_________:
			throw new RuntimeException("Error termination");
		case FGETC_________:
			node = (Node) stack[--sp];
			int p = ((Int) stack[--sp]).getNumber();
			int c = inputs.get(node).read(p);
			result = Int.create(c);
			break;
		case HEAD__________:
			result = Tree.decompose((Node) stack[--sp]).getLeft();
			break;
		case ISTREE________:
			result = atom(Tree.decompose((Node) stack[--sp]) != null);
			break;
		case ISVECTOR______:
			result = atom(stack[--sp] instanceof Vector);
			break;
		case LOG1__________:
			result = (Node) stack[--sp];
			LogUtil.info(Formatter.display(unwrap(result)));
			break;
		case LOG2__________:
			LogUtil.info(unwrapToString((Node) stack[--sp]));
			result = (Node) stack[--sp];
			break;
		case POPEN_________:
			Node n0 = unwrap((Node) stack[--sp]);
			Node n1 = (Node) stack[--sp];

			try {
				Process process = Runtime.getRuntime().exec(unwrapToString(n0));

				InputStreamReader reader = new InputStreamReader(process.getInputStream());
				inputs.put(result = Atom.unique(), new IndexedReader(reader));

				try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream())) {
					unwrap(n1, writer);
				}
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
			break;
		case PROVE_________:
			Prover prover = proverConfig != null ? new Prover(proverConfig) : Suite.createProver(Arrays.asList("auto.sl"));
			node = (Node) stack[--sp];
			tree = Tree.decompose(node, TermOp.JOIN__);

			if (tree != null)
				if (prover.prove(tree.getLeft()))
					result = tree.getRight().finalNode();
				else
					throw new RuntimeException("Goal failed");
			else
				result = prover.prove(node) ? Atom.TRUE : Atom.FALSE;
			break;
		case SUBST_________:
			Generalizer g = new Generalizer();
			g.setVariablePrefix("_");

			Node var = (Node) stack[--sp];
			tree = (Tree) g.generalize((Node) stack[--sp]);
			((Reference) tree.getRight()).bound(var);
			result = tree.getLeft();
			break;
		case TAIL__________:
			result = Tree.decompose((Node) stack[--sp]).getRight();
			break;
		case VCONCAT_______:
			Vector vector0 = (Vector) stack[--sp];
			Vector vector1 = (Vector) stack[--sp];
			result = Vector.concat(vector0, vector1);
			break;
		case VCONS_________:
			Node head = (Node) stack[--sp];
			Vector tail = (Vector) stack[--sp];
			result = Vector.cons(head, tail);
			break;
		case VELEM_________:
			result = new Vector((Node) stack[--sp]);
			break;
		case VEMPTY________:
			result = Vector.EMPTY;
			break;
		case VHEAD_________:
			result = ((Vector) stack[--sp]).get(0);
			break;
		case VRANGE________:
			Vector vector = (Vector) stack[--sp];
			int s = ((Int) stack[--sp]).getNumber();
			int e = ((Int) stack[--sp]).getNumber();
			result = vector.subVector(s, e);
			break;
		case VTAIL_________:
			result = ((Vector) stack[--sp]).subVector(1, 0);
			break;
		default:
			throw new RuntimeException("Unknown instruction " + insn);
		}

		exec.sp = sp;
		regs[insn.op0] = result;
	}

	@Override
	public void close() {
		for (IndexedInput input : inputs.values())
			input.close();
	}

	public void setProverConfig(ProverConfig proverConfig) {
		this.proverConfig = proverConfig;
	}

}
