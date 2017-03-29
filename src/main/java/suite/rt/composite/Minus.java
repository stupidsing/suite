package suite.rt.composite;

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
		List<RayHit> subjectRayHits = RayUtil.filterRayHits(subject.hit(ray)).sort(RayHit.comparator).toList();
		List<RayHit> objectRayHits = RayUtil.filterRayHits(object.hit(ray)).sort(RayHit.comparator).toList();
		return RayUtil.joinRayHits(subjectRayHits, objectRayHits, pair -> pair.t0 && !pair.t1);
	}

}
