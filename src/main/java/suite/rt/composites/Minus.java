package suite.rt.composites;

import java.util.Collections;
import java.util.List;

import suite.rt.RayTracer.Ray;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RayTrace;
import suite.rt.RayUtil;
import suite.util.FunUtil.Fun;
import suite.util.Pair;

public class Minus implements RayTrace {

	private RayTrace subject;
	private RayTrace object;

	public Minus(RayTrace subject, RayTrace object) {
		this.subject = subject;
		this.object = object;
	}

	@Override
	public List<RayHit> hit(Ray ray) {
		List<RayHit> subjectRayHits = RayUtil.filterRayHits(subject.hit(new Ray(ray.startPoint, ray.dir)));
		List<RayHit> objectRayHits = RayUtil.filterRayHits(object.hit(new Ray(ray.startPoint, ray.dir)));
		Collections.sort(subjectRayHits, RayHit.comparator);
		Collections.sort(objectRayHits, RayHit.comparator);

		return RayUtil.join(subjectRayHits, objectRayHits, new Fun<Pair<Boolean, Boolean>, Boolean>() {
			public Boolean apply(Pair<Boolean, Boolean> pair) {
				return pair.t0 && !pair.t1;
			}
		});
	}

}
