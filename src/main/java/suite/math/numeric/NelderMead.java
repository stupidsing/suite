package suite.math.numeric;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import suite.math.linalg.Vector;
import suite.primitive.DblPrimitives.Dbl_Obj;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.adt.pair.DblObjPair;
import suite.streamlet.Read;

// https://github.com/fchollet/nelder-mead/blob/master/nelder_mead.py#L83
public class NelderMead {

	private Vector vec = new Vector();

	public float[] nm(int dim, Obj_Dbl<float[]> fun) {
		var step = .1d;
		var reflectFactor = 1d; // alpha
		var expandFactor = 2d; // gamma
		var contractFactor = -.5d; // rho
		var reduceFactor = .5d; // sigma

		var xs = new float[dim];
		var pairs = new ArrayList<>(List.of(DblObjPair.of(fun.apply(xs), xs)));

		for (var i = 0; i < dim; i++) {
			var xs1 = vec.of(xs);
			xs1[i] += step;
			pairs.add(DblObjPair.of(fun.apply(xs1), xs1));
		}

		for (var iter = 0; iter < 1024; iter++) {
			pairs.sort(Comparator.comparing(pair -> pair.t0));

			var centroid = new float[dim];
			var ps_1 = pairs.size() - 1;

			for (var i = 0; i < ps_1; i++) {
				var pair = pairs.get(i);
				vec.addOn(centroid, pair.t1);
			}

			vec.scaleOn(centroid, 1d / ps_1);

			var first = pairs.get(0);
			var last0 = pairs.get(ps_1 - 0);
			var last1 = pairs.get(ps_1 - 1);

			Dbl_Obj<DblObjPair<float[]>> pairFun = factor -> {
				var xs_ = vec.add(centroid, vec.scaleOn(vec.sub(centroid, last0.t1), factor));
				return DblObjPair.of(fun.apply(xs_), xs_);
			};

			DblObjPair<float[]> reflectPair;
			DblObjPair<float[]> expandPair;
			DblObjPair<float[]> contractPair;

			if ((reflectPair = pairFun.apply(reflectFactor)) != null //
					&& first.t0 <= reflectPair.t0 //
					&& reflectPair.t0 < last1.t0) // reflection
				pairs.set(ps_1, reflectPair);
			else if (reflectPair.t0 < first.t0) // expansion
				if ((expandPair = pairFun.apply(expandFactor)) != null && expandPair.t0 < reflectPair.t0)
					pairs.set(ps_1, expandPair);
				else
					pairs.set(ps_1, reflectPair);
			else if ((contractPair = pairFun.apply(contractFactor)) != null && contractPair.t0 < last0.t0) // contraction
				pairs.set(ps_1, contractPair);
			else
				pairs = new ArrayList<>(Read.from(pairs).map(pair -> { // reduction
					var reduce = vec.add(centroid, vec.scaleOn(vec.sub(centroid, pair.t1), reduceFactor));
					return DblObjPair.of(fun.apply(reduce), reduce);
				}).toList());
		}

		return pairs.get(0).t1;
	}

}
