package suite.instructionexecutor;

import primal.adt.map.BiHashMap;
import primal.adt.map.BiMap;
import primal.fp.Funs.Iterate;
import primal.parser.Operator;
import suite.Suite;
import suite.node.Node;
import suite.node.io.TermOp;
import suite.node.util.Comparer;

import java.util.HashMap;
import java.util.Map;

import static primal.statics.Fail.fail;

public class InstructionUtil {

	private static BiMap<Insn, String> insnNames = new BiHashMap<>();
	private static Map<Operator, Insn> evalInsns = new HashMap<>();

	static {
		for (var insn : Insn.values())
			insnNames.put(insn, insn.name);

		evalInsns.put(TermOp.PLUS__, Insn.EVALADD_______);
		evalInsns.put(TermOp.DIVIDE, Insn.EVALDIV_______);
		evalInsns.put(TermOp.EQUAL_, Insn.EVALEQ________);
		evalInsns.put(TermOp.LE____, Insn.EVALLE________);
		evalInsns.put(TermOp.LT____, Insn.EVALLT________);
		evalInsns.put(TermOp.MULT__, Insn.EVALMUL_______);
		evalInsns.put(TermOp.MINUS_, Insn.EVALSUB_______);
		evalInsns.put(TermOp.MODULO, Insn.EVALMOD_______);
		evalInsns.put(TermOp.NOTEQ_, Insn.EVALNE________);
	}

	public enum Insn {
		ASSIGNCONST___("ASSIGN-CONSTANT"),
		ASSIGNFRAMEREG("ASSIGN-FRAME-REG"),
		ASSIGNINT_____("ASSIGN-INT"),
		ASSIGNRESULT__("ASSIGN-RESULT"),
		ASSIGNTHUNK___("ASSIGN-THUNK"),
		ASSIGNTHUNKRES("ASSIGN-THUNK-RESULT"),
		BACKUPCSP_____("BACKUP-CSP"),
		BACKUPDSP_____("BACKUP-DSP"),
		BIND__________("BIND"),
		BINDMARK______("BIND-MARK"),
		BINDUNDO______("BIND-UNDO"),
		CALL__________("CALL"),
		CALLINTRINSIC_("CALL-INTRINSIC"),
		CALLTHUNK_____("CALL-THUNK"),
		COMPARE_______("COMPARE"),
		CONSLIST______("CONS-LIST"),
		CONSPAIR______("CONS-PAIR"),
		DATACHARS_____("DATA-CHARS"),
		DECOMPOSETREE0("DECOMPOSE-TREE0"),
		DECOMPOSETREE1("DECOMPOSE-TREE1"),
		ENTER_________("ENTER"),
		ERROR_________("ERROR"),
		EVALADD_______("EVAL-ADD"),
		EVALDIV_______("EVAL-DIV"),
		EVALEQ________("EVAL-EQ"),
		EVALLE________("EVAL-LE"),
		EVALLT________("EVAL-LT"),
		EVALMOD_______("EVAL-MOD"),
		EVALMUL_______("EVAL-MUL"),
		EVALNE________("EVAL-NE"),
		EVALSUB_______("EVAL-SUB"),
		EXIT__________("EXIT"),
		FORMTREE0_____("FORM-TREE0"),
		FORMTREE1_____("FORM-TREE1"),
		FRAMEBEGIN____("FRAME-BEGIN"),
		FRAMEEND______("FRAME-END"),
		GETINTRINSIC__("GET-INTRINSIC"),
		HEAD__________("HEAD"),
		IFFALSE_______("IF-FALSE"),
		IFLE__________("IF-LE"),
		IFLT__________("IF-LT"),
		IFNOTCONS_____("IF-NOT-CONS"),
		IFNOTPAIR_____("IF-NOT-PAIR"),
		IFNOTEQUALS___("IF-NOT-EQ"),
		ISCONS________("IS-CONS"),
		JUMP__________("JUMP"),
		JUMPCLOSURE___("JUMP-CLOSURE"),
		LOGREG________("LOG-REG"),
		LEAVE_________("LEAVE"),
		NEWNODE_______("NEW-NODE"),
		POP___________("POP"),
		POPANY________("POP-ANY"),
		PROVEINTERPRET("PROVE-INTERPRET"),
		PROVESYS______("PROVE-SYS"),
		PUSH__________("PUSH"),
		REMARK________("REMARK"),
		RESTORECSP____("RESTORE-CSP"),
		RESTOREDSP____("RESTORE-DSP"),
		RETURN________("RETURN"),
		SETRESULT_____("SET-RESULT"),
		TAIL__________("TAIL"),
		TOP___________("TOP"),
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

	protected static class Activation extends Thunk {
		protected Activation previous;
		protected int depth;

		protected Activation(Thunk thunk, Activation previous) {
			this(thunk.frame, thunk.ip, previous);
		}

		protected Activation(Frame frame, int ip, Activation previous) {
			super(frame, ip);
			this.previous = previous;
			depth = previous != null ? 1 + previous.depth : 0;
			if (Suite.stackSize < depth)
				fail("activation overflow");
		}
	}

	// indicates a function call with a specified set of framed environment.
	// thunk must extend Node in order to be put in a register.
	protected static class Thunk extends Node {
		protected Frame frame;
		protected int ip;
		protected Node result;

		protected Thunk(Frame frame, int ip) {
			this.frame = frame;
			this.ip = ip;
		}

		public String toString() {
			return "frameSize = " + frame.registers.length + ", IP = " + ip;
		}
	}

	protected static class Frame {
		protected Frame previous;
		protected Node[] registers;

		protected Frame(Frame previous, int frameSize) {
			this(previous, new Node[frameSize]);
		}

		protected Frame(Frame previous, Node[] registers) {
			this.previous = previous;
			this.registers = registers;
		}
	}

	public static class FunComparer extends Comparer {
		private Iterate<Node> yawn;

		public FunComparer(Iterate<Node> yawn) {
			this.yawn = yawn;
		}

		public int compare(Node n0, Node n1) {
			return super.compare(yawn.apply(n0), yawn.apply(n1));
		}
	}

	public static Insn getEvalInsn(TermOp operator) {
		return InstructionUtil.evalInsns.get(operator);
	}

	public static Insn getInsn(String insnName) {
		return InstructionUtil.insnNames.inverse().get(insnName);
	}

}
