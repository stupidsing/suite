package suite.rt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import suite.math.Vector;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RayTraceObject;

public class Scene implements RayTraceObject {

	private Collection<RayTraceObject> objects;

	public Scene(Collection<RayTraceObject> objects) {
		this.objects = objects;
	}

	@Override
	public RayHit hit(Vector startPoint, Vector direction) {
		List<RayHit> hits = new ArrayList<>();

		for (RayTraceObject object : objects) {
			RayHit hit = object.hit(startPoint, direction);

			if (hit != null)
				hits.add(hit);
		}

		if (!hits.isEmpty())
			return Collections.min(hits, new Comparator<RayHit>() {
				public int compare(RayHit h0, RayHit h1) {
					return h0.advance() < h1.advance() ? -1 : 1;
				}
			});
		else
			return null;
	}

}
