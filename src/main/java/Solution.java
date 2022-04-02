import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;

public class Solution {

	public static void main(String[] args) {
		new Solution().run(args);
	}

	void run(String[] args) {
		Scanner in = new Scanner(new BufferedReader(new InputStreamReader(System.in)));
		int t = in.nextInt();
		for (int c = 1; c <= t; c++) {
			String out;

			{
				int m = in.nextInt();
				int n = in.nextInt();
				out = "" + (m + n);
			}

			System.out.println("Case #" + c + ": " + out);
			System.out.flush();
		}
		in.close();
	}

}
