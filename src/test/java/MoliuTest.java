import org.junit.Test;

public class MoliuTest {

	@Test
	public void test() {
		for (int x = -19; x <= 19; x++) {
			for (int y = -19; y <= 19; y++) {
				int x_ = Math.abs(x);
				int y_ = Math.abs(y);
				int diag = Math.min(x_, y_);
				int d = diag * 3 + (x_ - diag) * 2 + (y_ - diag) * 2;
				if (37 < d)
					System.out.print("OO");
				else
					System.out.print("__");
			}
			System.out.println();
		}
	}

}
