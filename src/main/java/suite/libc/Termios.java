package suite.libc;

import java.io.Closeable;
import java.nio.ByteBuffer;

import suite.util.Util;

public class Termios implements Closeable {

	private char esc = (char) 27;
	private Libc libc = Libc.instance;
	private ByteBuffer termios0 = ByteBuffer.allocateDirect(4096);

	private Thread hook = new Thread(this::close);

	public Termios() {
		ByteBuffer termios1 = ByteBuffer.allocateDirect(4096);
		libc.tcgetattr(0, termios0);
		libc.tcgetattr(0, termios1);
		libc.cfmakeraw(termios1);
		libc.tcsetattr(0, 0, termios1);
		Runtime.getRuntime().addShutdownHook(hook);
	}

	@Override
	public void close() {
		Runtime.getRuntime().removeShutdownHook(hook);
		showCursor();
		libc.tcsetattr(0, 1, termios0); // TCSADRAIN
	}

	public void clear() {
		puts(esc + "[2J");
		gotoxy(0, 0);
	}

	public void hideCursor() {
		puts(esc + "[?25l");
	}

	public void showCursor() {
		puts(esc + "[?25h");
	}

	public void gotoxy(int x, int y) {
		puts(esc + "[" + (y + 1) + ";" + (x + 1) + "H");
	}

	public void puts(String s) {
		for (char ch : Util.chars(s))
			libc.putchar(ch);
	}

}
