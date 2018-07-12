package suite.lp;

import java.util.ArrayList;
import java.util.List;

import suite.node.Node;
import suite.node.Reference;

public class Trail {

	private List<Reference> boundReferences = new ArrayList<>();

	public void addBind(Reference reference, Node target) {
		if (target instanceof Reference) {
			var reference1 = (Reference) target;
			if (reference.getId() < reference1.getId())
				addDirectedBind(reference1, reference);
			else
				addDirectedBind(reference, reference1);
		} else
			addDirectedBind(reference, target);
	}

	public void addDirectedBind(Reference reference, Node target) {
		boundReferences.add(reference);
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
		while (pointInTime < i)
			boundReferences.remove(--i).unbound();
	}

}
