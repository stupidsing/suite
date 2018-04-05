package suite.lcs;

import java.util.ArrayDeque;
import java.util.ArrayList;
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
		var nm = size0 + size1;

		int d, i1 = 0;
		int[] vdummy = new int[] { 0, };
		int[] v0 = vdummy, v1 = null;
		var k1 = 0;
		int x2 = 0, y2 = 0;

		List<int[]> vs = new ArrayList<>();

		found: for (d = 0; d <= nm; d++) {
			v1 = new int[d + 1];
			vs.add(v1);

			for (i1 = 0; i1 <= d; i1++) {
				k1 = i1 * 2 - d;

				// move down or move right?
				var down = k1 == -d || k1 != d && v0[i1 - 1] < v0[i1];
				var i0 = i1 + (down ? 0 : -1);

				// moves like a snake; down or right for 1 step, then
				// diagonals
				var x0 = v0[i0];
				var x1 = x0 + (down ? 0 : 1);
				x2 = x1;
				y2 = x2 - k1;

				while (x2 < size0 && y2 < size1 && list0.get(x2) == list1.get(y2)) {
					x2++;
					y2++;
				}

				// saves end point
				v1[i1] = x2;

				// solution found?
				if (size0 <= x2 && size1 <= y2)
					break found;
			}

			v0 = v1;
		}

		var deque = new ArrayDeque<>();

		for (; 0 < x2 || 0 < y2; d--) {
			v0 = 0 < d ? vs.get(d - 1) : vdummy;

			// move down or move right?
			var down = k1 == -d || k1 != d && v0[i1 - 1] < v0[i1];
			var i0 = i1 + (down ? 0 : -1);

			// moves back the snake
			var x0 = v0[i0];
			var x1 = x0 + (down ? 0 : 1);
			x2 = v1[i1];

			// saves end point
			while (x1 < x2)
				deque.addFirst(list0.get(--x2));

			v1 = v0;
			i1 = i0;
			k1 += down ? 1 : -1;
			x2 = x0;
			y2 = x2 - k1;
		}

		return new ArrayList<>(deque);
	}

}
