package suite.rt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import suite.rt.RayTracer.Ray;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RayTraceObject;
import suite.util.FunUtil.Fun;
import suite.util.Pair;

public class Composites {

	public class Intersect implements RayTraceObject {
		private Collection<RayTraceObject> objects;

		public Intersect(Collection<RayTraceObject> objects) {
			this.objects = objects;
		}

		public List<RayHit> hit(Ray ray) {
			return join(objects, ray, new Fun<Pair<Boolean, Boolean>, Boolean>() {
				public Boolean apply(Pair<Boolean, Boolean> pair) {
					return pair.t0 && pair.t1;
				}
			});
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
			Collections.sort(subjectRayHits, RayHit.comparator);
			Collections.sort(objectRayHits, RayHit.comparator);

			return join(subjectRayHits, objectRayHits, new Fun<Pair<Boolean, Boolean>, Boolean>() {
				public Boolean apply(Pair<Boolean, Boolean> pair) {
					return pair.t0 && !pair.t1;
				}
			});
		}
	}

	public class Union implements RayTraceObject {
		private Collection<RayTraceObject> objects;

		public Union(Collection<RayTraceObject> objects) {
			this.objects = objects;
		}

		public List<RayHit> hit(Ray ray) {
			return join(objects, ray, new Fun<Pair<Boolean, Boolean>, Boolean>() {
				public Boolean apply(Pair<Boolean, Boolean> pair) {
					return pair.t0 || pair.t1;
				}
			});
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

	private List<RayHit> join(Collection<RayTraceObject> objects, Ray ray, Fun<Pair<Boolean, Boolean>, Boolean> fun) {
		List<List<RayHit>> rayHitsList = getRayHitsList(ray, objects);
		List<RayHit> rayHits = !rayHitsList.isEmpty() ? rayHitsList.get(0) : Collections.<RayHit> emptyList();

		for (int i = 1; i < rayHitsList.size(); i++)
			rayHits = join(rayHits, rayHitsList.get(i), fun);

		return rayHits;
	}

	private List<RayHit> join(List<RayHit> rayHits0, List<RayHit> rayHits1, Fun<Pair<Boolean, Boolean>, Boolean> fun) {
		List<RayHit> rayHits2 = new ArrayList<>();
		boolean isInside0 = false, isInside1 = false;
		int size0 = rayHits0.size(), size1 = rayHits1.size();
		int index0 = 0, index1 = 0;
		boolean isInsideNow = false;

		while (index0 < size0 && index1 < size1) {
			RayHit rayHit0 = index0 < size0 ? rayHits1.get(index0) : null;
			RayHit rayHit1 = index1 < size1 ? rayHits1.get(index1) : null;
			boolean isAdvance0 = rayHit0 != null ? rayHit1 != null ? rayHit0.advance() < rayHit1.advance() : true : false;

			if (isAdvance0) {
				isInside0 = !isInside0;
				index0++;
			} else {
				isInside1 = !isInside1;
				index1++;
			}

			boolean isInsideBefore = isInsideNow;
			isInsideNow = fun.apply(Pair.create(isInside0, isInside1));

			if (isInsideBefore != isInsideNow)
				rayHits2.add(isAdvance0 ? rayHit0 : rayHit1);
		}

		// Eliminate duplicates
		return eliminateDuplicates(rayHits2);
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
