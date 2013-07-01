package org.instructionexecutor;

import org.instructionexecutor.InstructionUtil.Activation;
import org.instructionexecutor.InstructionUtil.Frame;
import org.instructionexecutor.InstructionUtil.Instruction;
import org.suite.Journal;
import org.suite.doer.Binder;
import org.suite.doer.Prover;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Tree;
import org.suite.predicates.SystemPredicates;

public class LogicInstructionExecutor extends InstructionExecutor {

	private Prover prover;
	private Journal journal;
	private SystemPredicates systemPredicates;

	public LogicInstructionExecutor(Node node, Prover prover) {
		super(node);
		this.prover = prover;
		journal = prover.getJournal();
		systemPredicates = new SystemPredicates(prover);
	}

	@Override
	protected void handle(Exec exec, Instruction insn) {
		Activation current = exec.current;
		Frame frame = current.frame;
		Node regs[] = frame != null ? frame.registers : null;
		Instruction insn1;

		switch (insn.insn) {
		case BACKUPCSP_____:
			regs[insn.op0] = exec.current.previous;
			break;
		case BIND__________:
			if (!Binder.bind(regs[insn.op0], regs[insn.op1], journal))
				current.ip = insn.op2; // Fail
			break;
		case BINDMARK______:
			regs[insn.op0] = number(journal.getPointInTime());
			break;
		case BINDUNDO______:
			journal.undoBinds(i(regs[insn.op0]));
			break;
		case DECOMPOSETREE0:
			Node node = regs[insn.op0].finalNode();

			insn1 = getInstructions()[current.ip++];
			TermOp op = TermOp.find(((Atom) constantPool.get(insn1.op0)).getName());
			int rl = insn1.op1;
			int rr = insn1.op2;

			if (node instanceof Tree) {
				Tree tree = (Tree) node;

				if (tree.getOperator() == op) {
					regs[rl] = tree.getLeft();
					regs[rr] = tree.getRight();
				} else
					current.ip = insn.op1;
			} else if (node instanceof Reference) {
				Tree tree = Tree.create(op, regs[rl] = new Reference(), regs[rr] = new Reference());
				journal.addBind((Reference) node, tree);
			} else
				current.ip = insn.op1;
			break;
		case PROVEINTERPRET:
			if (!prover.prove(regs[insn.op0]))
				current.ip = insn.op1;
			break;
		case PROVESYS______:
			if (!systemPredicates.call(regs[insn.op0]))
				current.ip = insn.op1;
			break;
		case RESTORECSP____:
			exec.current.previous = (Activation) regs[insn.op0];
			break;
		default:
			throw new RuntimeException("Unknown instruction " + insn);
		}
	}

}
