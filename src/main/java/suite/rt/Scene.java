package suite.rt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import suite.rt.RayTracer.Ray;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RayTraceObject;

public class Scene implements RayTraceObject {

	private Collection<RayTraceObject> objects;

	public Scene(Collection<RayTraceObject> objects) {
		this.objects = objects;
	}

	@Override
	public List<RayHit> hit(Ray ray) {
		List<RayHit> rayHits = new ArrayList<>();

		for (RayTraceObject object : objects)
			rayHits.addAll(object.hit(new Ray(ray.startPoint, ray.dir)));

		return rayHits;
	}

}
