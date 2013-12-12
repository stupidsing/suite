package suite.immutable;

import java.util.Comparator;

import org.junit.Test;

import suite.util.To;

public class Tree23Test {

	private int max = 32;

	private Comparator<Integer> comparator = new Comparator<Integer>() {
		public int compare(Integer i0, Integer i1) {
			return i0 - i1;
		}
	};

	@Test
	public void test0() {
		Tree23<Integer> tree23 = new Tree23<>(comparator);

		for (int i = 0; i < max; i++)
			tree23 = tree23.add(i);

		System.out.println(tree23.toString());
		for (int i = 0; i < max; i++)
			tree23 = tree23.remove(i);

		System.out.println(tree23.toString());
	}

	@Test
	public void test1() {
		Tree23<Integer> tree23 = new Tree23<>(comparator);

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

	private void dump(Tree23<Integer> tree23) {
		System.out.print("LIST = ");
		for (Integer i : To.list(tree23.iterator()))
			System.out.print(i + " ");
		System.out.println();
	}

}
