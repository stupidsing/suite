package suite.rt.composite;

import suite.rt.RayHit_;
import suite.rt.RayTracer.Ray;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RtObject;

import java.util.Collection;
import java.util.List;

public class Union implements RtObject {

	private Collection<RtObject> objects;

	public Union(Collection<RtObject> objects) {
		this.objects = objects;
	}

	@Override
	public List<RayHit> hit(Ray ray) {
		return RayHit_.join(objects, ray, pair -> pair.k || pair.v);
	}

}
