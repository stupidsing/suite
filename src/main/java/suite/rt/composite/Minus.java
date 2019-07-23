package suite.rt.composite;

import java.util.List;

import suite.rt.RayHit_;
import suite.rt.RayTracer.Ray;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RtObject;

public class Minus implements RtObject {

	private RtObject subject;
	private RtObject object;

	public Minus(RtObject subject, RtObject object) {
		this.subject = subject;
		this.object = object;
	}

	@Override
	public List<RayHit> hit(Ray ray) {
		var subjectRayHits = RayHit_.filter(subject.hit(ray)).sort(RayHit.comparator).toList();
		var objectRayHits = RayHit_.filter(object.hit(ray)).sort(RayHit.comparator).toList();
		return RayHit_.join(subjectRayHits, objectRayHits, pair -> pair.k && !pair.v);
	}

}
