package suite.lp;

import java.util.ArrayList;
import java.util.List;

import primal.adt.Pair;
import suite.node.Node;
import suite.node.Reference;

public class Trail {

	private List<Pair<Reference, Node>> boundReferences = new ArrayList<>();

	public void addBind(Reference reference, Node target) {
		if (target instanceof Reference reference1)
			if (reference.getId() < reference1.getId())
				addDirectedBind(reference1, reference);
			else
				addDirectedBind(reference, reference1);
		else
			addDirectedBind(reference, target);
	}

	public void addDirectedBind(Reference reference, Node target) {
		boundReferences.add(Pair.of(reference, reference.node));
		reference.bound(target);
	}

	public int getPointInTime() {
		return boundReferences.size();
	}

	public void unwindAll() {
		unwind(0);
	}

	public void unwind(int pointInTime) {
		var i = boundReferences.size();
		while (pointInTime < i) {
			var boundReference = boundReferences.remove(--i);
			boundReference.k.bound(boundReference.v);
		}
	}

}
