package suite.lp;

import java.util.ArrayList;
import java.util.List;

import suite.node.Node;
import suite.node.Reference;

public class Journal {

	private List<Reference> bounded = new ArrayList<>();

	public void addBind(Reference reference, Node target) {
		if (target instanceof Reference && reference.getId() < ((Reference) target).getId())
			addDirectedBind((Reference) target, reference);
		else
			addDirectedBind(reference, target);
	}

	private void addDirectedBind(Reference reference, Node target) {
		bounded.add(reference);
		reference.bound(target);
	}

	public int getPointInTime() {
		return bounded.size();
	}

	public void undoAllBinds() {
		undoBinds(0);
	}

	public void undoBinds(int pointInTime) {
		int i = bounded.size();
		while (i > pointInTime)
			bounded.remove(--i).unbound();
	}

	public List<Reference> getBinded() {
		return bounded;
	}

}
