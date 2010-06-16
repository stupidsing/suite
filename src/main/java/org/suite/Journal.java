package org.suite;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.suite.node.Node;
import org.suite.node.Reference;

public class Journal {

	private List<Reference> bounded = new ArrayList<Reference>();

	public void addBind(Reference reference, Node target) {
		bounded.add(reference);
		reference.bound(target);
	}

	public int getPointInTime() {
		return bounded.size();
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
