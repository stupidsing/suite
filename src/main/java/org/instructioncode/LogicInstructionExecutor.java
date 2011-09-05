package org.instructioncode;

import java.util.ArrayList;
import java.util.List;

import org.suite.Binder;
import org.suite.Journal;
import org.suite.node.Node;
import org.util.Util;
import org.util.Util.Pair;

public class LogicInstructionExecutor extends InstructionExecutor {

	private final static int STACKSIZE = 256;

	private Journal journal = new Journal();
	private int bindPoints[] = new int[STACKSIZE];
	private List<CutPoint> cutPoints = new ArrayList<CutPoint>();
	private int bsp = 0;

	public LogicInstructionExecutor(Node node) {
		super(node);
	}

	@Override
	protected Pair<Integer, Integer> execute(Closure current, Instruction insn,
			Closure callStack[], int csp, Object dataStack[], int dsp) {
		Frame frame = current.frame;
		Object regs[] = frame != null ? frame.registers : null;

		switch (insn.insn) {
		case BIND__________:
			bindPoints[bsp++] = journal.getPointInTime();
			if (!Binder.bind( //
					(Node) regs[insn.op1], (Node) regs[insn.op2], journal))
				current.ip = insn.op3; // Fail
			break;
		case BINDUNDO______:
			journal.undoBinds(bindPoints[--bsp]);
			break;
		case CUTBEGIN______:
			regs[insn.op1] = i(cutPoints.size());
			cutPoints.add(new CutPoint(journal.getPointInTime(), csp));
			break;
		case CUTEND________:
			int p = cutPoints.get(g(regs[insn.op1])).callStackPointer;
			while (csp > p)
				callStack[--csp] = null;
			break;
		case CUTFAIL_______:
			int cutPointIndex = g(regs[insn.op1]);
			CutPoint cutPoint = cutPoints.get(cutPointIndex);
			journal.undoBinds(cutPoint.journalPointer);
			Util.truncate(cutPoints, cutPointIndex);
			current.ip = insn.op2;
			break;
		default:
			return super.execute(current, insn, callStack, csp, dataStack, dsp);
		}

		return Pair.create(csp, dsp);
	}

}
