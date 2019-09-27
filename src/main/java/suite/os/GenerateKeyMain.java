package suite.os;

import static primal.statics.Rethrow.ex;

import suite.util.RunUtil;

// gcc -std=c99 -g src/main/c/encb.c -o target/encb && mvn -Dmaven.test.skip=true install assembly:single && (find src/ -type f | xargs cat | java -cp target/suite-1.0-jar-with-dependencies.jar suite.os.GenerateKeyMain | target/encb)
public class GenerateKeyMain {

	private int size = 32;

	public static void main(String[] args) {
		RunUtil.run(new GenerateKeyMain()::run);
	}

	private boolean run() {
		try (var os = System.out) {
			var bs = new byte[size];
			var bs_ = new byte[size];
			int n;

			while (0 < (n = ex(() -> System.in.read(bs_))))
				for (var i = 0; i < n; i++) {
					var b = Integer.rotateLeft(bs[i], 3) + bs_[i];
					// var b = bs[i] ^ bs_[i];
					bs[i] = (byte) b;
				}

			for (var i = 0; i < bs.length; i++)
				System.out.write(bs[i]);

			return true;
		}
	}

}
