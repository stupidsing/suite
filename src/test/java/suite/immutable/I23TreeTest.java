package suite.immutable;

import java.util.Comparator;

import org.junit.Test;

import suite.util.FunUtil;

public class I23TreeTest {

	private int max = 32;

	private Comparator<Integer> comparator = new Comparator<Integer>() {
		public int compare(Integer i0, Integer i1) {
			return i0 - i1;
		}
	};

	@Test
	public void test0() {
		I23Tree<Integer> tree23 = new I23Tree<>(comparator);

		for (int i = 0; i < max; i++)
			tree23 = tree23.add(i);

		System.out.println(tree23.toString());
		for (int i = 0; i < max; i++)
			tree23 = tree23.remove(i);

		System.out.println(tree23.toString());
	}

	@Test
	public void test1() {
		I23Tree<Integer> tree23 = new I23Tree<>(comparator);

		System.out.println(tree23.toString());
		dump(tree23);

		for (int i = 0; i < max; i += 2)
			tree23 = tree23.add(i);
		for (int i = 1; i < max; i += 2)
			tree23 = tree23.add(i);

		System.out.println(tree23.toString());
		dump(tree23);

		for (int i = 0; i < max; i += 2)
			tree23 = tree23.remove(i);
		for (int i = 1; i < max; i += 2)
			tree23 = tree23.remove(i);
	}

	private void dump(I23Tree<Integer> tree23) {
		System.out.print("LIST = ");
		for (Integer i : FunUtil.iter(tree23.source()))
			System.out.print(i + " ");
		System.out.println();
	}

}
