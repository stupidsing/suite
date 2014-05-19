package suite.libc;

import java.io.Closeable;
import java.nio.ByteBuffer;

public class Termios implements Closeable {

	private static Libc libc = Libc.instance;
	private ByteBuffer termios0 = ByteBuffer.allocateDirect(4096);

	public Termios() {
		ByteBuffer termios1 = ByteBuffer.allocateDirect(4096);
		libc.tcgetattr(0, termios0);
		libc.tcgetattr(0, termios1);
		libc.cfmakeraw(termios1);
		libc.tcsetattr(0, 0, termios1);
	}

	@Override
	public void close() {
		libc.tcsetattr(0, 1, termios0); // TCSADRAIN
	}

}
