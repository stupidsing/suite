package suite.libc;

import java.io.IOException;

import suite.libc.Termios.AnsiColor;

// java -cp target/suite-1.0-jar-with-dependencies.jar suite.libc.TermiosMain
public class TermiosMain {

	public static void main(String args[]) throws IOException {
		try (Termios termios = new Termios()) {
			termios.clear();
			termios.hideCursor();

			termios.background(AnsiColor.GREEN_);
			termios.foreground(AnsiColor.RED___);
			termios.puts("test red on green");
			termios.resetColors();

			for (int r = 0; r < 6; r++)
				for (int g = 0; g < 6; g++)
					for (int b = 0; b < 6; b++) {
						termios.gotoxy(b + 8 * r, g + 1);
						termios.background(r, g, b);
						termios.puts(" ");
					}

			termios.resetColors();

			int ch;
			while ((ch = Libc.getchar()) != -1 && ch != 'q')
				System.out.println(ch);
		}
	}

}
