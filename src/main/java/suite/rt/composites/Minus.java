package suite.rt.composites;

import java.util.Collections;
import java.util.List;

import suite.rt.RayTracer.Ray;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RtObject;
import suite.rt.RayUtil;

public class Minus implements RtObject {

	private RtObject subject;
	private RtObject object;

	public Minus(RtObject subject, RtObject object) {
		this.subject = subject;
		this.object = object;
	}

	@Override
	public List<RayHit> hit(Ray ray) {
		List<RayHit> subjectRayHits = RayUtil.filterRayHits(subject.hit(new Ray(ray.startPoint, ray.dir)));
		List<RayHit> objectRayHits = RayUtil.filterRayHits(object.hit(new Ray(ray.startPoint, ray.dir)));
		Collections.sort(subjectRayHits, RayHit.comparator);
		Collections.sort(objectRayHits, RayHit.comparator);
		return RayUtil.joinRayHits(subjectRayHits, objectRayHits, pair -> pair.t0 && !pair.t1);
	}

}
