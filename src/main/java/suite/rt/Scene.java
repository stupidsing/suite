package suite.rt;

import suite.rt.RayTracer.Ray;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RtObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Scene implements RtObject {

	private Collection<RtObject> objects;

	public Scene(Collection<RtObject> objects) {
		this.objects = objects;
	}

	@Override
	public List<RayHit> hit(Ray ray) {
		var rayHits = new ArrayList<RayHit>();

		for (var object : objects)
			rayHits.addAll(object.hit(new Ray(ray.startPoint, ray.dir)));

		return rayHits;
	}

}
