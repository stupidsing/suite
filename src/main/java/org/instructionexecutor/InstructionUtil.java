package org.instructionexecutor;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.instructionexecutor.io.IndexedIo;
import org.parser.Operator;
import org.suite.Suite;
import org.suite.doer.Comparer;
import org.suite.doer.Generalizer;
import org.suite.doer.Prover;
import org.suite.doer.ProverConfig;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Tree;
import org.util.FunUtil.Fun;
import org.util.LogUtil;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class InstructionUtil {

	private static final BiMap<Insn, String> insnNames = HashBiMap.create();
	private static final Map<Operator, Insn> evalInsns = new HashMap<>();

	static {
		for (Insn insn : Insn.values())
			insnNames.put(insn, insn.name);

		evalInsns.put(TermOp.PLUS__, Insn.EVALADD_______);
		evalInsns.put(TermOp.DIVIDE, Insn.EVALDIV_______);
		evalInsns.put(TermOp.EQUAL_, Insn.EVALEQ________);
		evalInsns.put(TermOp.GE____, Insn.EVALGE________);
		evalInsns.put(TermOp.GT____, Insn.EVALGT________);
		evalInsns.put(TermOp.LE____, Insn.EVALLE________);
		evalInsns.put(TermOp.LT____, Insn.EVALLT________);
		evalInsns.put(TermOp.MULT__, Insn.EVALMUL_______);
		evalInsns.put(TermOp.MINUS_, Insn.EVALSUB_______);
		evalInsns.put(TermOp.MODULO, Insn.EVALMOD_______);
		evalInsns.put(TermOp.NOTEQ_, Insn.EVALNE________);
	}

	public enum Insn {
		ASSIGNCLOSURE_("ASSIGN-CLOSURE"), //
		ASSIGNCONST___("ASSIGN-CONSTANT"), //
		ASSIGNFRAMEREG("ASSIGN-FRAME-REG"), //
		ASSIGNGLOBAL__("ASSIGN-GLOBAL"), //
		ASSIGNINT_____("ASSIGN-INT"), //
		BIND__________("BIND"), //
		BINDMARK______("BIND-MARK"), //
		BINDUNDO______("BIND-UNDO"), //
		CALL__________("CALL"), //
		CALLCONST_____("CALL-CONSTANT"), //
		CALLCLOSURE___("CALL-CLOSURE"), //
		COMPARE_______("COMPARE"), //
		CONS__________("CONS"), //
		CUTBEGIN______("CUT-BEGIN"), //
		CUTFAIL_______("CUT-FAIL"), //
		DECOMPOSETREE0("DECOMPOSE-TREE0"), //
		DECOMPOSETREE1("DECOMPOSE-TREE1"), //
		ENTER_________("ENTER"), //
		ERROR_________("ERROR"), //
		EVALADD_______("EVAL-ADD"), //
		EVALDIV_______("EVAL-DIV"), //
		EVALEQ________("EVAL-EQ"), //
		EVALGE________("EVAL-GE"), //
		EVALGT________("EVAL-GT"), //
		EVALLE________("EVAL-LE"), //
		EVALLT________("EVAL-LT"), //
		EVALMOD_______("EVAL-MOD"), //
		EVALMUL_______("EVAL-MUL"), //
		EVALNE________("EVAL-NE"), //
		EVALSUB_______("EVAL-SUB"), //
		EXIT__________("EXIT"), //
		FGETC_________("FGETC"), //
		FORMTREE0_____("FORM-TREE0"), //
		FORMTREE1_____("FORM-TREE1"), //
		HEAD__________("HEAD"), //
		IFFALSE_______("IF-FALSE"), //
		IFGE__________("IF-GE"), //
		IFGT__________("IF-GT"), //
		IFLE__________("IF-LE"), //
		IFLT__________("IF-LT"), //
		IFNOTEQUALS___("IF-NOT-EQ"), //
		ISTREE________("IS-TREE"), //
		ISVECTOR______("IS-VECTOR"), //
		JUMP__________("JUMP"), //
		LABEL_________("LABEL"), //
		LOG___________("LOG"), //
		LOG1__________("LOG1"), //
		LOG2__________("LOG2"), //
		LEAVE_________("LEAVE"), //
		NEWNODE_______("NEW-NODE"), //
		POP___________("POP"), //
		POPEN_________("POPEN"), //
		PROVE_________("PROVE"), //
		PROVEINTERPRET("PROVE-INTERPRET"), //
		PROVESYS______("PROVE-SYS"), //
		PUSH__________("PUSH"), //
		PUSHCONST_____("PUSH-CONSTANT"), //
		REMARK________("REMARK"), //
		RETURN________("RETURN"), //
		RETURNVALUE___("RETURN-VALUE"), //
		SETCLOSURERES_("SET-CLOSURE-RESULT"), //
		SETRESULT_____("SET-RESULT"), //
		STOREGLOBAL___("STORE-GLOBAL"), //
		SUBST_________("SUBST"), //
		TAIL__________("TAIL"), //
		TOP___________("TOP"), //
		VCONCAT_______("VCONCAT"), //
		VCONS_________("VCONS"), //
		VELEM_________("VELEM"), //
		VEMPTY________("VEMPTY"), //
		VHEAD_________("VHEAD"), //
		VRANGE________("VRANGE"), //
		VTAIL_________("VTAIL"), //
		;

		String name;

		private Insn(String name) {
			this.name = name;
		}
	}

	protected static class Instruction {
		protected Insn insn;
		protected int op0, op1, op2;

		protected Instruction(Insn insn, int op0, int op1, int op2) {
			this.insn = insn;
			this.op0 = op0;
			this.op1 = op1;
			this.op2 = op2;
		}

		public String toString() {
			return insn.name + " " + op0 + ", " + op1 + ", " + op2;
		}
	}

	protected static class CutPoint {
		protected Activation activation;
		protected int journalPointer;

		protected CutPoint(Activation activation, int journalPointer) {
			this.activation = activation;
			this.journalPointer = journalPointer;
		}
	}

	protected static class Activation extends Closure {
		protected Activation previous;

		protected Activation(Closure closure, Activation previous) {
			this(closure.frame, closure.ip, previous);
		}

		protected Activation(Frame frame, int ip, Activation previous) {
			super(frame, ip);
			this.previous = previous;
		}
	}

	// Indicates a function call with a specified set of framed environment.
	// Closure must extend Node in order to be put in a list (being cons-ed).
	protected static class Closure extends Node {
		protected Frame frame;
		protected int ip;
		protected Node result;

		protected Closure(Frame frame, int ip) {
			this.frame = frame;
			this.ip = ip;
		}

		public String toString() {
			return "frameSize = " + frame.registers.length + ", IP = " + ip;
		}
	}

	protected static class Frame {
		protected Frame previous;
		protected Node registers[];

		protected Frame(Frame previous, int frameSize) {
			this.previous = previous;
			registers = new Node[frameSize];
		}
	}

	public static class FunComparer extends Comparer {
		private Fun<Node, Node> unwrapper;

		public FunComparer(Fun<Node, Node> unwrapper) {
			this.unwrapper = unwrapper;
		}

		public int compare(Node n0, Node n1) {
			return super.compare(unwrapper.apply(n0), unwrapper.apply(n1));
		}
	}

	public static List<Node> extractTuple(Node node) {
		List<Node> rs = new ArrayList<>(5);
		Tree tree;

		while ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null) {
			rs.add(tree.getLeft());
			node = tree.getRight();
		}

		rs.add(node);
		return rs;
	}

	public static Insn getEvalInsn(TermOp operator) {
		return InstructionUtil.evalInsns.get(operator);
	}

	public static Insn getInsn(String insnName) {
		return InstructionUtil.insnNames.inverse().get(insnName);
	}

	public static Node execPopen(Node n0, final Node n1, IndexedIo indexedIo, final Fun<Node, Node> unwrapper) {
		try {
			Node result = Atom.unique();
			final Process process = Runtime.getRuntime().exec(ExpandUtil.expandString(n0, unwrapper));

			indexedIo.put(result, new InputStreamReader(process.getInputStream()));

			// Use a separate thread to write to the process, so that read and
			// write occur at the same time and would not block up.
			// Have to make sure the executors are thread-safe!
			new Thread() {
				public void run() {
					try (OutputStream pos = process.getOutputStream(); Writer writer = new OutputStreamWriter(pos)) {
						ExpandUtil.expand(n1, unwrapper, writer);
					} catch (IOException ex) {
						LogUtil.error(ex);
					}
				}
			}.start();

			return result;
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static Node execProve(Node node, ProverConfig proverConfig) {
		Prover prover = proverConfig != null ? new Prover(proverConfig) : Suite.createProver(Arrays.asList("auto.sl"));
		Tree tree = Tree.decompose(node, TermOp.JOIN__);
		Node result;

		if (tree != null)
			if (prover.prove(tree.getLeft()))
				result = tree.getRight().finalNode();
			else
				throw new RuntimeException("Goal failed");
		else
			result = prover.prove(node) ? Atom.TRUE : Atom.FALSE;

		return result;
	}

	public static Node execSubst(Node node, Node var) {
		Generalizer generalizer = new Generalizer();
		generalizer.setVariablePrefix("_");

		Tree tree = (Tree) generalizer.generalize(node);
		((Reference) tree.getRight()).bound(var);
		return tree.getLeft();
	}

}
