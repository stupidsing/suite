import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;

public class Solution {

	public static void main(String[] args) {
		Scanner in = new Scanner(new BufferedReader(new InputStreamReader(System.in)));
		int t = in.nextInt(); // Scanner has functions to read ints, longs, strings, chars, etc.
		for (int c = 1; c <= t; ++c) {
			int m = in.nextInt();
			int n = in.nextInt();
			String out = "" + (m + n);
			System.out.println("Case #" + c + ": " + out);
		}
		in.close();
	}

}
