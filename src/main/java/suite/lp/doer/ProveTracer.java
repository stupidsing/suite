package suite.lp.doer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import suite.Suite;
import suite.lp.Configuration.TraceLevel;
import suite.node.Data;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.TermOp;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil.Source;

public class ProveTracer {

	private List<Record> records = new ArrayList<>();
	private Record currentRecord = null;
	private int currentDepth;

	private class Record {
		private int start, end = -1;
		private Record parent;
		private Node query;
		private int depth;
		private int nOkays;

		private Record(Record parent, Node query, int depth) {
			this.parent = parent;
			this.query = query;
			this.depth = depth;
		}

		private void appendTo(StringBuilder sb) {
			TraceLevel traceLevel = Suite.traceLevel;

			if (traceLevel == TraceLevel.SIMPLE)
				sb.append(String.format("[%4s:%-2d]  ", 0 < nOkays ? "OK__" : "FAIL", depth));
			else if (traceLevel == TraceLevel.DETAIL)
				sb.append(String.format("%-4d[up=%-4d|oks=%-2d|end=%-4s]  " //
						, start //
						, parent != null ? parent.start : 0 //
						, nOkays //
						, 0 <= end ? String.valueOf(end) : ""));

			for (int i = 1; i < depth; i++)
				sb.append("| ");
			sb.append(Formatter.dump(query));
			sb.append("\n");
		}
	}

	public Node expandWithTrace(Node query, Prover prover, Iterate<Node> expand) {
		Node query1 = new Cloner().clone(query);

		if (currentDepth < 64) {
			Record record0 = currentRecord;
			int depth0 = currentDepth;
			Record record = new Record(record0, query1, currentDepth + 1);

			Data<Source<Boolean>> enter = new Data<>(() -> {
				currentRecord = record;
				currentDepth = record.depth;
				record.start = records.size();
				records.add(record);
				return Boolean.TRUE;
			});

			Data<Source<Boolean>> leaveOk = new Data<>(() -> {
				currentRecord = record0;
				currentDepth = depth0;
				record.nOkays++;
				return Boolean.TRUE;
			});

			Data<Source<Boolean>> leaveFail = new Data<>(() -> {
				currentRecord = record0;
				currentDepth = depth0;
				record.end = records.size();
				return Boolean.FALSE;
			});

			Node alt = prover.getAlternative();
			Node rem = prover.getRemaining();

			prover.setAlternative(Tree.of(TermOp.OR____, leaveFail, alt));
			prover.setRemaining(Tree.of(TermOp.AND___, leaveOk, rem));

			query = expand.apply(query);
			query = Tree.of(TermOp.AND___, enter, query);
		} else
			query = expand.apply(query);

		return query;
	}

	public String getTrace() {
		return log(records);
	}

	public String getStackTrace() {
		return log(currentRecord);
	}

	public String getFailTrace() {
		if (!records.isEmpty() && records.get(0).nOkays == 0)
			return log(Collections.max(records, (record0, record1) -> {
				int depth0 = isDecidinglyFail(record0) ? record0.depth : 0;
				int depth1 = isDecidinglyFail(record1) ? record1.depth : 0;
				return depth0 - depth1;
			}));
		else
			return "-";
	}

	private boolean isDecidinglyFail(Record record) {
		while (record != null)
			if (record.nOkays == 0)
				record = record.parent;
			else
				return false;
		return true;
	}

	private String log(Record record) {
		Deque<Record> deque = new ArrayDeque<>();
		while (record != null) {
			deque.addFirst(record);
			record = record.parent;
		}
		return log(new ArrayList<>(deque));
	}

	private String log(List<Record> records) {
		int size = records.size();

		// this method could be invoked in shutdown hook and the prover might
		// still be running. Do not use iterator/for-each loop access, those
		// would cause ConcurrentModificationException.
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < size; i++)
			records.get(i).appendTo(sb);
		return sb.toString();
	}

}
