package suite.lp;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

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
		ListIterator<Reference> iter = bounded.listIterator(pointInTime);
		while (iter.hasNext()) {
			iter.next().unbound();
			iter.remove();
		}
	}

	public List<Reference> getBinded() {
		return bounded;
	}

}
