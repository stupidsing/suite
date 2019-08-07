package suite.math.linalg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import primal.primitive.adt.map.IntObjMap;
import suite.node.util.Singleton;
import suite.streamlet.As;

public class LinearDiscriminantAnalysisTest {

	private LinearDiscriminantAnalysis lda = new LinearDiscriminantAnalysis();
	private Vector vec = new Vector();

	@Test
	public void test() {
		var categoryCounter = new AtomicInteger();
		var categoryIdByName = new HashMap<String, Integer>();
		var dataByCategoryId = new IntObjMap<List<float[]>>();

		Singleton.me.storeCache //
				.http("https://archive.ics.uci.edu/ml/machine-learning-databases/iris/iris.data") //
				.collect(As::csv) //
				.sink(t -> {
					var category = categoryIdByName.computeIfAbsent(t[4], c -> categoryCounter.getAndIncrement());

					dataByCategoryId.computeIfAbsent(category, c -> new ArrayList<>()).add(new float[] { //
							Float.valueOf(t[0]), //
							Float.valueOf(t[1]), //
							Float.valueOf(t[2]), //
							Float.valueOf(t[3]), });
				});

		var nCategories = dataByCategoryId.size();
		var data = new float[nCategories][][];

		for (var c = 0; c < nCategories; c++)
			data[c] = dataByCategoryId.get(c).toArray(new float[0][]);

		var fun = lda.lda(2, data);
		var eps = .001f;

		vec.verifyEquals(fun.apply(new float[] { 1f, 0f, 0f, 0f, }), new float[] { -.2049f, -.009f, }, eps);
		vec.verifyEquals(fun.apply(new float[] { 0f, 1f, 0f, 0f, }), new float[] { -.3871f, -.589f, }, eps);
		vec.verifyEquals(fun.apply(new float[] { 0f, 0f, 1f, 0f, }), new float[] { .5465f, .2543f, }, eps);
		vec.verifyEquals(fun.apply(new float[] { 0f, 0f, 0f, 1f, }), new float[] { .7138f, -.767f, }, eps);
	}

}
