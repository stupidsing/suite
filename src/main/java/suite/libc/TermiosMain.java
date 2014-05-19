package suite.libc;

import java.io.IOException;

// java -cp target/suite-1.0-jar-with-dependencies.jar suite.sample.Termios
public class TermiosMain {

	public static void main(String args[]) throws IOException {
		try (Termios termios = new Termios()) {
			termios.clear();

			int ch;
			while ((ch = Libc.instance.getchar()) != -1 && ch != 'q')
				System.out.println(ch);
		}
	}

}
