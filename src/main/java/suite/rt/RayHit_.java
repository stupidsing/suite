package suite.rt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import suite.adt.pair.Pair;
import suite.rt.RayTracer.Ray;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RtObject;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public class RayHit_ {

	/**
	 * Remove hits that are shooting backwards.
	 */
	public static Streamlet<RayHit> filter(List<RayHit> rayHits) {
		return filter_(rayHits);
	}

	public static List<RayHit> join(Collection<RtObject> objects, Ray ray, Fun<Pair<Boolean, Boolean>, Boolean> fun) {
		var rayHitsList = getHits(ray, objects);
		List<RayHit> rayHits = !rayHitsList.isEmpty() ? rayHitsList.get(0) : List.of();
		for (var i = 1; i < rayHitsList.size(); i++)
			rayHits = join(rayHits, rayHitsList.get(i), fun);
		return rayHits;
	}

	public static List<RayHit> join(List<RayHit> rayHits0, List<RayHit> rayHits1, Fun<Pair<Boolean, Boolean>, Boolean> fun) {
		var rayHits2 = new ArrayList<RayHit>();
		int size0 = rayHits0.size(), size1 = rayHits1.size();
		int index0 = 0, index1 = 0;
		boolean b0, b1;
		var isInsideNow = false;

		while ((b0 = index0 < size0) | (b1 = index1 < size1)) {
			RayHit rayHit0 = b0 ? rayHits0.get(index0) : null;
			RayHit rayHit1 = b1 ? rayHits1.get(index1) : null;
			var isAdvance0 = b0 && (!b1 || rayHit0.advance() < rayHit1.advance());

			if (isAdvance0)
				index0++;
			else
				index1++;

			var isInsideBefore = isInsideNow;
			isInsideNow = fun.apply(Pair.of(index0 % 2 == 1, index1 % 2 == 1));

			if (isInsideBefore != isInsideNow)
				rayHits2.add(isAdvance0 ? rayHit0 : rayHit1);
		}

		return removeDuplicates(rayHits2);
	}

	private static List<List<RayHit>> getHits(Ray ray, Collection<RtObject> objects) {
		return Read //
				.from(objects) //
				.map(object -> filter_(object.hit(ray)).sort(RayHit.comparator).toList()) //
				.toList();
	}

	private static List<RayHit> removeDuplicates(List<RayHit> rayHits0) {
		var rayHits1 = new ArrayList<RayHit>();
		var size = rayHits0.size();
		RayHit rayHit;

		for (var i = 0; i < size; i++)
			if (i != size - 1)
				if ((rayHit = rayHits0.get(i)) != rayHits0.get(i + 1))
					rayHits1.add(rayHit);
				else
					i++; // skips same hit
			else
				rayHits1.add(rayHits0.get(i));

		return rayHits1;
	}

	private static Streamlet<RayHit> filter_(List<RayHit> rayHits) {
		return Read.from(rayHits).filter(rh -> 0 < rh.advance());
	}

}
