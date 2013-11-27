package suite.rt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import suite.rt.RayTracer.Ray;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RayTrace;
import suite.util.FunUtil.Fun;
import suite.util.Pair;

public class RayUtil {

	/**
	 * Remove hits that are shooting backwards.
	 */
	public static List<RayHit> filterRayHits(List<RayHit> rayHits) {
		List<RayHit> rayHits1 = new ArrayList<>();

		for (RayHit rayHit : rayHits)
			if (rayHit.advance() > 0)
				rayHits1.add(rayHit);

		return rayHits1;
	}

	public static List<RayHit> joinRayHits(Collection<RayTrace> objects, Ray ray, Fun<Pair<Boolean, Boolean>, Boolean> fun) {
		List<List<RayHit>> rayHitsList = getRayHitsList(ray, objects);
		List<RayHit> rayHits = !rayHitsList.isEmpty() ? rayHitsList.get(0) : Collections.<RayHit> emptyList();

		for (int i = 1; i < rayHitsList.size(); i++)
			rayHits = joinRayHits(rayHits, rayHitsList.get(i), fun);

		return rayHits;
	}

	public static List<RayHit> joinRayHits(List<RayHit> rayHits0, List<RayHit> rayHits1, Fun<Pair<Boolean, Boolean>, Boolean> fun) {
		List<RayHit> rayHits2 = new ArrayList<>();
		int size0 = rayHits0.size(), size1 = rayHits1.size();
		int index0 = 0, index1 = 0;
		boolean b0, b1;
		boolean isInsideNow = false;

		while ((b0 = index0 < size0) | (b1 = index1 < size1)) {
			RayHit rayHit0 = b0 ? rayHits0.get(index0) : null;
			RayHit rayHit1 = b1 ? rayHits1.get(index1) : null;
			boolean isAdvance0 = b0 ? b1 ? rayHit0.advance() < rayHit1.advance() : true : false;

			if (isAdvance0)
				index0++;
			else
				index1++;

			boolean isInsideBefore = isInsideNow;
			isInsideNow = fun.apply(Pair.create(index0 % 2 == 1, index1 % 2 == 1));

			if (isInsideBefore != isInsideNow)
				rayHits2.add(isAdvance0 ? rayHit0 : rayHit1);
		}

		// Eliminate duplicates
		return eliminateRayHitsDuplicates(rayHits2);
	}

	private static List<List<RayHit>> getRayHitsList(Ray ray, Collection<RayTrace> objects) {
		List<List<RayHit>> rayHitsList = new ArrayList<>();

		for (RayTrace object : objects) {
			List<RayHit> rayHits = RayUtil.filterRayHits(object.hit(new Ray(ray.startPoint, ray.dir)));
			Collections.sort(rayHits, RayHit.comparator);
			rayHitsList.add(rayHits);
		}

		return rayHitsList;
	}

	private static List<RayHit> eliminateRayHitsDuplicates(List<RayHit> rayHits0) {
		List<RayHit> rayHits1 = new ArrayList<>();
		int size = rayHits0.size();
		RayHit rayHit;

		for (int i = 0; i < size; i++)
			if (i != size - 1)
				if ((rayHit = rayHits0.get(i)) != rayHits0.get(i + 1))
					rayHits1.add(rayHit);
				else
					i++; // Skips same hit
			else
				rayHits1.add(rayHits0.get(i));

		return rayHits1;
	}

}
