import java.util.Scanner;

public class SolutionInteractive {

	public static void solve(Scanner input, int a, int b) {
		int m = (a + b) / 2;
		System.out.println(m);
		String s = input.next();
		if (s.equals("CORRECT"))
			;
		else if (s.equals("TOO_SMALL"))
			solve(input, m + 1, b);
		else
			solve(input, a, m - 1);
	}

	public static void main(String args[]) {
		Scanner input = new Scanner(System.in);
		int T = input.nextInt();
		for (int ks = 1; ks <= T; ks++) {
			int a = input.nextInt();
			int b = input.nextInt();
			input.nextInt();
			solve(input, a + 1, b);
		}
	}

}
