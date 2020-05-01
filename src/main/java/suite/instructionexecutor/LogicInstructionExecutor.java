package suite.instructionexecutor;

import suite.instructionexecutor.InstructionUtil.Activation;
import suite.instructionexecutor.InstructionUtil.Instruction;
import suite.lp.Configuration.ProverCfg;
import suite.lp.doer.Binder;
import suite.lp.doer.Prover;
import suite.lp.predicate.SystemPredicates;
import suite.node.*;
import suite.node.io.TermOp;

import static primal.statics.Fail.fail;

public class LogicInstructionExecutor extends InstructionExecutor {

	private Prover prover;
	private SystemPredicates systemPredicates;

	public LogicInstructionExecutor(Node node, ProverCfg proverCfg) {
		super(node);
		prover = new Prover(proverCfg);
		systemPredicates = new SystemPredicates(prover);
	}

	@Override
	protected void handle(Exec exec, Instruction insn) {
		var current = exec.current;
		var frame = current.frame;
		var regs = frame != null ? frame.registers : null;
		var trail = prover.getTrail();
		Instruction insn1;

		switch (insn.insn) {
		case BACKUPCSP_____ -> {
			regs[insn.op0] = exec.current.previous;
		}
		case BACKUPDSP_____ -> {
			regs[insn.op0] = number(exec.sp);
		}
		case BIND__________ -> {
			if (!Binder.bind(regs[insn.op0], regs[insn.op1], trail))
				current.ip = insn.op2; // fail
		}
		case BINDMARK______ -> {
			regs[insn.op0] = number(trail.getPointInTime());
		}
		case BINDUNDO______ -> {
			trail.unwind(Int.num(regs[insn.op0]));
		}
		case DECOMPOSETREE0 -> {
			var node = regs[insn.op0].finalNode();

			insn1 = getInstructions()[current.ip++];
			var op = TermOp.find(Atom.name(constantPool.get(insn1.op0)));
			var rl = insn1.op1;
			var rr = insn1.op2;

			if (node instanceof Tree) {
				var tree = (Tree) node;

				if (tree.getOperator() == op) {
					regs[rl] = tree.getLeft();
					regs[rr] = tree.getRight();
				} else
					current.ip = insn.op1;
			} else if (node instanceof Reference) {
				Tree tree = Tree.of(op, regs[rl] = new Reference(), regs[rr] = new Reference());
				trail.addBind((Reference) node, tree);
			} else
				current.ip = insn.op1;
		}
		case PROVEINTERPRET -> {
			if (!prover.prove(regs[insn.op0]))
				current.ip = insn.op1;
		}
		case PROVESYS______ -> {
			if (!systemPredicates.call(regs[insn.op0]))
				current.ip = insn.op1;
		}
		case RESTORECSP____ -> {
			exec.current.previous = (Activation) regs[insn.op0];
		}
		case RESTOREDSP____ -> {
			exec.sp = Int.num(regs[insn.op0]);
		}
		default -> {
			fail("unknown instruction " + insn);
		}
		}
	}

}
