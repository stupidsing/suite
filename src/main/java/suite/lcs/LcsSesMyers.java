package suite.lcs;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * Myer's algorithm for longest common subsequence/shortest edit script.
 * 
 * Code copied shamelessly from: http://simplygenius.net/Article/DiffTutorial1
 * 
 * Not even implemented part 2, linear space refinement...
 * 
 * @author ywsing
 */
public class LcsSesMyers<T> {

	public List<T> myers(List<T> list0, List<T> list1) {
		int size0 = list0.size(), size1 = list1.size();
		int nm = size0 + size1;

		int v0[] = new int[2 * nm + 1], v1[] = null;
		int k0, k1 = 0;
		int x2 = 0, y2 = 0;

		Arrays.fill(v0, Integer.MIN_VALUE);
		v0[nm + 1] = 0;

		List<int[]> vs = new ArrayList<>();
		vs.add(v0);

		found: for (int d = 0; d <= nm; d++) {
			v1 = new int[2 * nm + 1];
			vs.add(v1);

			for (k1 = -d; k1 <= d; k1 += 2) {

				// Move down or move right?
				boolean down = k1 == -d || k1 != d && v0[nm + k1 - 1] < v0[nm + k1 + 1];
				k0 = down ? k1 + 1 : k1 - 1;

				// Moves like a snake; down or right for 1 step, then
				// diagonals
				int x0 = v0[nm + k0];
				int x1 = down ? x0 : x0 + 1;
				x2 = x1;
				y2 = x2 - k1;

				while (x2 < size0 && y2 < size1 && list0.get(x2) == list1.get(y2)) {
					x2++;
					y2++;
				}

				// Saves end point
				v1[nm + k1] = x2;

				// Solution found?
				if (x2 >= size0 && y2 >= size1)
					break found;
			}

			v0 = v1;
		}

		Deque<T> deque = new ArrayDeque<>();

		for (int d = vs.size() - 1; x2 > 0 || y2 > 0; d--) {
			v0 = vs.get(d - 1);

			// Move down or move right?
			boolean down = k1 == -d || k1 != d && v0[nm + k1 - 1] < v0[nm + k1 + 1];
			k0 = down ? k1 + 1 : k1 - 1;

			// Moves back the snake
			int x0 = v0[nm + k0];
			int x1 = down ? x0 : x0 + 1;
			x2 = v1[nm + k1];

			// Saves end point
			while (x2 > x1)
				deque.addFirst(list0.get(--x2));

			v1 = v0;
			k1 = k0;
			x2 = x0;
			y2 = x2 - k1;
		}

		return new ArrayList<>(deque);
	}

}
