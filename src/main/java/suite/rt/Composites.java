package suite.rt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import suite.rt.RayTracer.Ray;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RayIntersection;
import suite.rt.RayTracer.RayTraceObject;

public class Composites {

	public class Union implements RayTraceObject {
		private Collection<RayTraceObject> objects;

		public Union(Collection<RayTraceObject> objects) {
			this.objects = objects;
		}

		public List<RayHit> hit(Ray ray) {
			List<List<RayHit>> rayHitsList = getRayHitsList(ray, objects);
			List<RayHit> rayHits = !rayHitsList.isEmpty() ? rayHitsList.get(0) : Collections.<RayHit> emptyList();

			for (int i = 1; i < rayHitsList.size(); i++)
				rayHits = union(rayHits, rayHitsList.get(i));

			return rayHits;
		}
	}

	public class Intersect implements RayTraceObject {
		private Collection<RayTraceObject> objects;

		public Intersect(Collection<RayTraceObject> objects) {
			this.objects = objects;
		}

		public List<RayHit> hit(Ray ray) {
			List<List<RayHit>> rayHitsList = getRayHitsList(ray, objects);
			List<RayHit> rayHits = !rayHitsList.isEmpty() ? rayHitsList.get(0) : Collections.<RayHit> emptyList();

			for (int i = 1; i < rayHitsList.size(); i++)
				rayHits = intersect(rayHits, rayHitsList.get(i));

			return rayHits;
		}
	}

	public class Minus implements RayTraceObject {
		private RayTraceObject subject;
		private RayTraceObject object;

		public Minus(RayTraceObject subject, RayTraceObject object) {
			this.subject = subject;
			this.object = object;
		}

		public List<RayHit> hit(Ray ray) {
			List<RayHit> subjectRayHits = RayTracer.filterRayHits(subject.hit(new Ray(ray.startPoint, ray.dir)));
			List<RayHit> objectRayHits = RayTracer.filterRayHits(object.hit(new Ray(ray.startPoint, ray.dir)));

			// Adds a dummy and negate the hits
			objectRayHits.add(new RayHit() {
				public float advance() {
					return 0f;
				}

				public RayIntersection intersection() {
					return null;
				}
			});

			Collections.sort(subjectRayHits, RayHit.comparator);
			Collections.sort(objectRayHits, RayHit.comparator);

			return intersect(subjectRayHits, objectRayHits);
		}
	}

	private List<List<RayHit>> getRayHitsList(Ray ray, Collection<RayTraceObject> objects) {
		List<List<RayHit>> rayHitsList = new ArrayList<>();

		for (RayTraceObject object : objects) {
			List<RayHit> rayHits = RayTracer.filterRayHits(object.hit(new Ray(ray.startPoint, ray.dir)));
			Collections.sort(rayHits, RayHit.comparator);
			rayHitsList.add(rayHits);
		}
		return rayHitsList;
	}

	private List<RayHit> intersect(List<RayHit> rayHits, List<RayHit> intersectRayHits) {
		return join(rayHits, intersectRayHits, false);
	}

	private List<RayHit> union(List<RayHit> rayHits, List<RayHit> intersectRayHits) {
		return join(rayHits, intersectRayHits, true);
	}

	private List<RayHit> join(List<RayHit> rayHits, List<RayHit> joinRayHits, boolean isUnion) {
		List<RayHit> rayHits1 = new ArrayList<>();
		boolean isInside = false;
		RayHit rayHit;
		int i = 0, j = 0;

		while (i < joinRayHits.size()) {
			RayHit joinRayHit = joinRayHits.get(i);

			while (j < rayHits.size() && (rayHit = rayHits.get(j)).advance() < joinRayHit.advance()) {
				rayHits1.add(isInside ? rayHit : joinRayHit);
				j++;
			}

			if (isUnion)
				rayHits1.add(joinRayHit);

			isInside = !isInside;
			i++;
		}

		// Eliminate duplicates
		return eliminateDuplicates(rayHits1);
	}

	private List<RayHit> eliminateDuplicates(List<RayHit> rayHits1) {
		List<RayHit> rayHits = new ArrayList<>();
		RayHit rayHit;

		for (int i = 0; i < rayHits.size(); i++)
			if (i != rayHits.size() - 1)
				if ((rayHit = rayHits.get(i)) == rayHits.get(i + 1))
					rayHits.add(rayHit);
				else
					i++; // Skips same hit
			else
				rayHits.add(rayHits.get(i));

		return rayHits;
	}

}
