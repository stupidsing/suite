package suite.rt.composites;

import java.util.Collection;
import java.util.List;

import suite.rt.RayTracer.Ray;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RtObject;
import suite.rt.RayUtil;
import suite.util.FunUtil.Fun;
import suite.util.Pair;

public class Intersect implements RtObject {

	private Collection<RtObject> objects;

	public Intersect(Collection<RtObject> objects) {
		this.objects = objects;
	}

	@Override
	public List<RayHit> hit(Ray ray) {
		return RayUtil.joinRayHits(objects, ray, new Fun<Pair<Boolean, Boolean>, Boolean>() {
			public Boolean apply(Pair<Boolean, Boolean> pair) {
				return pair.t0 && pair.t1;
			}
		});
	}

}
