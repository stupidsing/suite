package suite.ansi;

import com.sun.jna.Native;

import suite.ansi.Termios.AnsiColor;
import suite.util.Thread_;

// mvn compile exec:java -Dexec.mainClass=suite.ansi.TermiosMain
public class TermiosMain {

	private LibcJna libc = (LibcJna) Native.loadLibrary("c", LibcJna.class);

	public static void main(String[] args) {
		new TermiosMain().run();
	}

	private void run() {
		try (Termios termios = new Termios(libc)) {
			termios.clear();
			termios.cursor(false);

			termios.background(AnsiColor.GREEN_);
			termios.foreground(AnsiColor.RED___);
			termios.puts("test red on green");

			for (int i = 0; i < 40; i++) {
				termios.resetColors();
				termios.clear();

				for (int r = 0; r < 6; r++)
					for (int g = 0; g < 6; g++)
						for (int b = 0; b < 6; b++) {
							termios.gotoxy(b + 8 * r, g + 1);
							termios.background(r, g, b);
							termios.puts(" ");
						}

				termios.resetColors();

				termios.gotoxy(i, i + 8);
				termios.puts("string is moving on its way\n");
				Thread_.sleepQuietly(300l);
			}

			termios.resetColors();
			termios.reportMouse();

			int ch;
			while ((ch = libc.getchar()) != -1 && ch != 'q')
				System.out.println(ch);
		}
	}

}
