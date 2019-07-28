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

	public float[] nm(Obj_Dbl<float[]> fun, int dim) {
		var step = .1d;
		var reflectFactor = 1d; // alpha
		var expandFactor = 2d; // gamma
		var contractFactor = -.5d; // rho
		var reduceFactor = .5d; // sigma

		var xs = new float[dim];
		var pairs = new ArrayList<>(List.of(DblObjPair.of(fun.apply(xs), xs)));

		for (var i = 0; i < dim; i++) {
			var xs1 = vec.copyOf(xs);
			xs1[i] += step;
			pairs.add(DblObjPair.of(fun.apply(xs1), xs1));
		}

		for (var iter = 0; iter < 1024; iter++) {
			pairs.sort(Comparator.comparing(pair -> pair.k));

			var centroid = new float[dim];
			var ps_1 = pairs.size() - 1;

			for (var i = 0; i < ps_1; i++) {
				var pair = pairs.get(i);
				vec.addOn(centroid, pair.v);
			}

			vec.scaleOn(centroid, 1d / ps_1);

			var first = pairs.get(0);
			var last0 = pairs.get(ps_1 - 0);
			var last1 = pairs.get(ps_1 - 1);

			Dbl_Obj<DblObjPair<float[]>> pairFun = factor -> {
				var xs_ = vec.add(centroid, vec.scaleOn(vec.sub(centroid, last0.v), factor));
				return DblObjPair.of(fun.apply(xs_), xs_);
			};

			DblObjPair<float[]> reflectPair;
			DblObjPair<float[]> expandPair;
			DblObjPair<float[]> contractPair;

			if ((reflectPair = pairFun.apply(reflectFactor)) != null //
					&& first.k <= reflectPair.k //
					&& reflectPair.k < last1.k) // reflection
				pairs.set(ps_1, reflectPair);
			else if (reflectPair.k < first.k) // expansion
				if ((expandPair = pairFun.apply(expandFactor)) != null && expandPair.k < reflectPair.k)
					pairs.set(ps_1, expandPair);
				else
					pairs.set(ps_1, reflectPair);
			else if ((contractPair = pairFun.apply(contractFactor)) != null && contractPair.k < last0.k) // contraction
				pairs.set(ps_1, contractPair);
			else
				pairs = new ArrayList<>(Read.from(pairs).map(pair -> { // reduction
					var reduce = vec.add(first.v, vec.scaleOn(vec.sub(pair.v, first.v), reduceFactor));
					return DblObjPair.of(fun.apply(reduce), reduce);
				}).toList());
		}

		return pairs.get(0).v;
	}

}
