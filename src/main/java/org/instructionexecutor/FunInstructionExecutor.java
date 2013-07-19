package org.instructionexecutor;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.instructionexecutor.InstructionUtil.Activation;
import org.instructionexecutor.InstructionUtil.Closure;
import org.instructionexecutor.InstructionUtil.Frame;
import org.instructionexecutor.InstructionUtil.FunComparer;
import org.instructionexecutor.InstructionUtil.Instruction;
import org.instructionexecutor.io.IndexedIo;
import org.suite.doer.Comparer;
import org.suite.doer.Formatter;
import org.suite.doer.ProverConfig;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Tree;
import org.suite.node.Vector;
import org.util.FunUtil.Fun;
import org.util.LogUtil;

public class FunInstructionExecutor extends InstructionExecutor {

	private ProverConfig proverConfig;
	private IndexedIo indexedIo = new IndexedIo();

	private Fun<Node, Node> unwrapper = new Fun<Node, Node>() {
		public Node apply(Node node) {
			node = node.finalNode();
			if (node instanceof Closure)
				node = evaluateClosure((Closure) node);
			return node;
		}
	};

	private Comparer comparer = new FunComparer(unwrapper);

	public FunInstructionExecutor(Node node) {
		super(node);
	}

	public void executeIo(Reader reader, Writer writer) throws IOException {
		indexedIo.put(Atom.NIL, reader);
		ExpandUtil.expand(execute(), unwrapper, writer);
	}

	@Override
	protected void handle(Exec exec, Instruction insn) {
		Activation current = exec.current;
		Frame frame = current.frame;
		Object regs[] = frame != null ? frame.registers : null;

		Object ds[] = exec.stack;
		int dsp = exec.sp;

		Node node, left, right, result;

		switch (insn.insn) {
		case COMPARE_______:
			left = (Node) ds[--dsp];
			right = (Node) ds[--dsp];

			result = Int.create(comparer.compare(left, right));
			break;
		case CONSLIST______:
			left = (Node) ds[--dsp];
			right = (Node) ds[--dsp];
			result = Tree.create(TermOp.OR____, left, right);
			break;
		case CONSPAIR______:
			left = (Node) ds[--dsp];
			right = (Node) ds[--dsp];
			result = Tree.create(TermOp.AND___, left, right);
			break;
		case ERROR_________:
			throw new RuntimeException("Error termination");
		case FGETC_________:
			node = (Node) ds[--dsp];
			int p = ((Int) ds[--dsp]).getNumber();
			int c = indexedIo.get(node).read(p);
			result = Int.create(c);
			break;
		case HEAD__________:
			result = Tree.decompose((Node) ds[--dsp]).getLeft();
			break;
		case ISTREE________:
			result = atom(Tree.decompose((Node) ds[--dsp]) != null);
			break;
		case ISVECTOR______:
			result = atom(ds[--dsp] instanceof Vector);
			break;
		case LOG1__________:
			result = (Node) ds[--dsp];
			LogUtil.info(Formatter.display(ExpandUtil.expand(result, unwrapper)));
			break;
		case LOG2__________:
			LogUtil.info(ExpandUtil.expandString((Node) ds[--dsp], unwrapper));
			result = (Node) ds[--dsp];
			break;
		case POPEN_________:
			Node n0 = (Node) ds[--dsp];
			Node n1 = (Node) ds[--dsp];
			result = InstructionUtil.execPopen(n0, n1, indexedIo, unwrapper);
			break;
		case PROVE_________:
			node = (Node) ds[--dsp];
			result = InstructionUtil.execProve(node, proverConfig);
			break;
		case SUBST_________:
			Node var = (Node) ds[--dsp];
			node = (Node) ds[--dsp];
			result = InstructionUtil.execSubst(node, var);
			break;
		case TAIL__________:
			result = Tree.decompose((Node) ds[--dsp]).getRight();
			break;
		case VCONCAT_______:
			Vector vector0 = (Vector) ds[--dsp];
			Vector vector1 = (Vector) ds[--dsp];
			result = Vector.concat(vector0, vector1);
			break;
		case VCONS_________:
			Node head = (Node) ds[--dsp];
			Vector tail = (Vector) ds[--dsp];
			result = Vector.cons(head, tail);
			break;
		case VELEM_________:
			result = new Vector((Node) ds[--dsp]);
			break;
		case VEMPTY________:
			result = Vector.EMPTY;
			break;
		case VHEAD_________:
			result = ((Vector) ds[--dsp]).get(0);
			break;
		case VRANGE________:
			Vector vector = (Vector) ds[--dsp];
			int s = ((Int) ds[--dsp]).getNumber();
			int e = ((Int) ds[--dsp]).getNumber();
			result = vector.range(s, e);
			break;
		case VTAIL_________:
			result = ((Vector) ds[--dsp]).range(1, 0);
			break;
		default:
			throw new RuntimeException("Unknown instruction " + insn);
		}

		exec.sp = dsp;
		regs[insn.op0] = result;
	}

	@Override
	protected Comparer comparer() {
		return comparer;
	}

	public Fun<Node, Node> getUnwrapper() {
		return unwrapper;
	}

	@Override
	public void close() {
		indexedIo.close();
	}

	public void setProverConfig(ProverConfig proverConfig) {
		this.proverConfig = proverConfig;
	}

}
