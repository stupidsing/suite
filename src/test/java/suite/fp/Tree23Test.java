package suite.fp;

import java.util.Comparator;

import org.junit.Test;

public class Tree23Test {

	@Test
	public void test() {
		Tree23<Integer> tree23 = new Tree23<>(new Comparator<Integer>() {
			public int compare(Integer i0, Integer i1) {
				return i0 - i1;
			}
		});

		int max = 32;

		for (int i = 0; i < max; i++)
			tree23 = tree23.add(i);

		System.out.println(tree23.toString());
		for (int i = 0; i < max; i++)
			tree23 = tree23.remove(i);

		System.out.println(tree23.toString());
	}

}
