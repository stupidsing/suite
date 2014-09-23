package suite.libc;

import java.io.Closeable;

import org.bridj.Pointer;

import suite.util.Util;

public class Termios implements Closeable {

	private char esc = (char) 27;
	private Pointer<Byte> termios0 = Pointer.allocateBytes(4096);

	private Thread hook = new Thread(this::close);

	public Termios() {
		Pointer<Byte> termios1 = Pointer.allocateBytes(4096);
		Libc.tcgetattr(0, termios0);
		Libc.tcgetattr(0, termios1);
		Libc.cfmakeraw(termios1);
		Libc.tcsetattr(0, 0, termios1);
		Runtime.getRuntime().addShutdownHook(hook);
	}

	@Override
	public void close() {
		Runtime.getRuntime().removeShutdownHook(hook);
		showCursor();
		Libc.tcsetattr(0, 1, termios0); // TCSADRAIN
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
			Libc.putchar(ch);
	}

}
