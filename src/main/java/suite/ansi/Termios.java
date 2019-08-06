package suite.ansi;

import java.io.Closeable;

import primal.Nouns.Buffer;
import suite.streamlet.Read;

public class Termios implements Closeable {

	private LibcJna libc;
	private char esc = (char) 27;
	private byte[] termios0 = new byte[Buffer.size];

	private Thread hook = new Thread(this::close);

	public enum AnsiColor {
		BLACK_(0), //
		RED___(1), //
		GREEN_(2), //
		YELLOW(3), //
		BLUE__(4), //
		MAGENT(5), //
		CYAN__(6), //
		WHITE_(7), //
		;

		public int value;

		private AnsiColor(int value) {
			this.value = value;
		}
	}

	public Termios(LibcJna libc) {
		this.libc = libc;

		var termios1 = new byte[Buffer.size];
		libc.tcgetattr(0, termios0);
		libc.tcgetattr(0, termios1);
		libc.cfmakeraw(termios1);
		libc.tcsetattr(0, 0, termios1);
		Runtime.getRuntime().addShutdownHook(hook);
	}

	@Override
	public void close() {
		Runtime.getRuntime().removeShutdownHook(hook);
		cursor(true);
		libc.tcsetattr(0, 1, termios0); // TCSADRAIN
	}

	public void clear() {
		puts(esc + "[2J");
		gotoxy(0, 0);
	}

	public void background(AnsiColor ac) {
		puts(esc + "[" + (ac.value + 40) + "m");
	}

	public void background(int r, int g, int b) {
		puts(esc + "[48;5;" + (16 + b + g * 6 + r * 36) + "m");
	}

	public void cursor(boolean isShow) {
		puts(esc + "[?25" + (isShow ? "h" : "l"));
	}

	public void foreground(AnsiColor ac) {
		puts(esc + "[" + (ac.value + 30) + "m");
	}

	public void foreground(int r, int g, int b) {
		puts(esc + "[38;5;" + (16 + b + g * 6 + r * 36) + "m");
	}

	public void gotoxy(int x, int y) {
		puts(esc + "[" + (y + 1) + ";" + (x + 1) + "H");
	}

	public void reportMouse() {
		puts(esc + "[?9h");
	}

	public void resetColors() {
		puts(esc + "[0m");
	}

	public void scroll(int dir) {
		puts(esc + "[r");
		scroll_(dir);
	}

	// dir = 1 for scrolling down
	public void scroll(int row0, int row1, int dir) {
		puts(esc + "[" + row0 + ";" + row1 + "r");
		scroll_(dir);
	}

	public void puts(String s) {
		Read.chars(s).forEach(libc::putchar);
	}

	private void scroll_(int dir) {
		while (dir < 0) {
			puts(esc + "M");
			dir++;
		}
		while (0 < dir) {
			puts(esc + "D");
			dir--;
		}
	}

}
