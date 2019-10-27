import org.junit.Test;

public class MoliuTest {

	public void moliuTest(int i) {
	}

	@Test
	public void test() {
		System.out.println(MoliuTest.class.getMethods()[0].getName());
		System.out.println(MoliuTest.class.getMethods()[0].getParameters()[0].getName());
	}

}
