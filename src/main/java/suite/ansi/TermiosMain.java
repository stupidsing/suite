package suite.ansi;

import com.sun.jna.Native;

import primal.Verbs.Sleep;
import suite.ansi.Termios.AnsiColor;

// mvn compile exec:java -Dexec.mainClass=suite.ansi.TermiosMain
public class TermiosMain {

	private LibcJna libc = (LibcJna) Native.load("c", LibcJna.class);

	public static void main(String[] args) {
		new TermiosMain().run();
	}

	private void run() {
		try (var termios = new Termios(libc)) {
			termios.clear();
			termios.cursor(false);

			termios.background(AnsiColor.GREEN_);
			termios.foreground(AnsiColor.RED___);
			termios.puts("test red on green");

			for (var i = 0; i < 3; i++) {
				termios.resetColors();
				termios.clear();

				for (var r = 0; r < 6; r++)
					for (var g = 0; g < 6; g++)
						for (var b = 0; b < 6; b++) {
							termios.gotoxy(b + 8 * r, g + 1);
							termios.background(r, g, b);
							termios.puts(" ");
						}

				termios.resetColors();

				termios.gotoxy(i, i + 8);
				termios.puts("string is moving on its way\n");
				Sleep.quietly(300l);
			}

			termios.resetColors();
			termios.reportMouse();

			int ch;
			while ((ch = libc.getchar()) != -1 && ch != 'q')
				System.out.println(ch);
		}
	}

}
