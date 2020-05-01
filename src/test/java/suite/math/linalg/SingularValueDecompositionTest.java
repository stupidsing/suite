package suite.math.linalg;

import org.junit.jupiter.api.Test;
import suite.util.To;

public class SingularValueDecompositionTest {

	private Matrix mtx = new Matrix();
	private SingularValueDecomposition svd = new SingularValueDecomposition();
	private Vector vec = new Vector();

	@Test
	public void test() {
		var fixie = svd.svd(mtx.identity(9));
		vec.verifyEquals(fixie.get0(), To.vector(9, i -> 1d), .01f);
	}

}
