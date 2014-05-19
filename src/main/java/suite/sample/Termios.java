package suite.sample;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.sun.jna.Library;
import com.sun.jna.Native;

// java -cp target/suite-1.0-jar-with-dependencies.jar suite.sample.Termios
public class Termios {

	private static Libc libc = (Libc) Native.loadLibrary("c", Libc.class);

	public interface Libc extends Library {
		public int getchar();

		public int ioctl(int d, int request, ByteBuffer data);

		public int tcgetattr(int fd, ByteBuffer termios_p);

		public int tcsetattr(int fd, int optional_actions, ByteBuffer termios_p);

		public int cfmakeraw(ByteBuffer termios_p);
	}

	public static void main(String args[]) throws IOException {
		ByteBuffer termios = ByteBuffer.allocateDirect(4096);
		libc.tcgetattr(0, termios);
		libc.cfmakeraw(termios);
		libc.tcsetattr(0, 1, termios);

		int ch;
		while ((ch = libc.getchar()) != -1 && ch != 'q')
			System.out.println(ch);
	}

}
