package suite.instructionexecutor;

import java.util.HashMap;
import java.util.Map;

import suite.Suite;
import suite.lp.intrinsic.Intrinsic;
import suite.node.Data;
import suite.node.Node;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.node.util.Comparer;
import suite.util.FunUtil.Fun;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class InstructionUtil {

	private static BiMap<Insn, String> insnNames = HashBiMap.create();
	private static Map<Operator, Insn> evalInsns = new HashMap<>();

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
		ASSIGNCLOSRES_("ASSIGN-CLOSURE-RESULT"), //
		ASSIGNCLOSURE_("ASSIGN-CLOSURE"), //
		ASSIGNCONST___("ASSIGN-CONSTANT"), //
		ASSIGNFRAMEREG("ASSIGN-FRAME-REG"), //
		ASSIGNINT_____("ASSIGN-INT"), //
		ASSIGNRESULT__("ASSIGN-RESULT"), //
		BACKUPCSP_____("BACKUP-CSP"), //
		BACKUPDSP_____("BACKUP-DSP"), //
		BIND__________("BIND"), //
		BINDMARK______("BIND-MARK"), //
		BINDUNDO______("BIND-UNDO"), //
		CALL__________("CALL"), //
		CALLCLOSURE___("CALL-CLOSURE"), //
		CALLINTRINSIC_("CALL-INTRINSIC"), //
		COMPARE_______("COMPARE"), //
		CONSLIST______("CONS-LIST"), //
		CONSPAIR______("CONS-PAIR"), //
		DECOMPOSETREE0("DECOMPOSE-TREE0"), //
		DECOMPOSETREE1("DECOMPOSE-TREE1"), //
		ENTER_________("ENTER"), //
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
		FORMTREE0_____("FORM-TREE0"), //
		FORMTREE1_____("FORM-TREE1"), //
		FRAMEBEGIN____("FRAME-BEGIN"), //
		FRAMEEND______("FRAME-END"), //
		GETINTRINSIC__("GET-INTRINSIC"), //
		HEAD__________("HEAD"), //
		IFFALSE_______("IF-FALSE"), //
		IFGE__________("IF-GE"), //
		IFGT__________("IF-GT"), //
		IFLE__________("IF-LE"), //
		IFLT__________("IF-LT"), //
		IFNOTEQUALS___("IF-NOT-EQ"), //
		ISCONS________("IS-CONS"), //
		JUMP__________("JUMP"), //
		JUMPCLOSURE___("JUMP-CLOSURE"), //
		LOGREG________("LOG-REG"), //
		LEAVE_________("LEAVE"), //
		NEWNODE_______("NEW-NODE"), //
		POP___________("POP"), //
		POPANY________("POP-ANY"), //
		PROVEINTERPRET("PROVE-INTERPRET"), //
		PROVESYS______("PROVE-SYS"), //
		PUSH__________("PUSH"), //
		REMARK________("REMARK"), //
		RESTORECSP____("RESTORE-CSP"), //
		RESTOREDSP____("RESTORE-DSP"), //
		RETURN________("RETURN"), //
		SETRESULT_____("SET-RESULT"), //
		TAIL__________("TAIL"), //
		TOP___________("TOP"), //
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

	protected static class Activation extends Closure {
		protected Activation previous;
		protected int depth;

		protected Activation(Closure closure, Activation previous) {
			this(closure.frame, closure.ip, previous);
		}

		protected Activation(Frame frame, int ip, Activation previous) {
			super(frame, ip);
			this.previous = previous;
			depth = previous != null ? 1 + previous.depth : 0;
			if (depth > Suite.stackSize)
				throw new RuntimeException("Activation overflow");
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
			this(previous, new Node[frameSize]);
		}

		protected Frame(Frame previous, Node registers[]) {
			this.previous = previous;
			this.registers = registers;
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

	public static Insn getEvalInsn(TermOp operator) {
		return InstructionUtil.evalInsns.get(operator);
	}

	public static Insn getInsn(String insnName) {
		return InstructionUtil.insnNames.inverse().get(insnName);
	}

	public static Data<Intrinsic> execGetIntrinsic(String intrinsicName) {
		Class<? extends Intrinsic> clazz;

		try {
			@SuppressWarnings("unchecked")
			Class<? extends Intrinsic> clazz0 = (Class<? extends Intrinsic>) Class.forName(intrinsicName);
			clazz = clazz0;
		} catch (ClassNotFoundException ex1) {
			throw new RuntimeException(ex1);
		}

		try {
			return new Data<>(clazz.newInstance());
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException(ex);
		}
	}

}
