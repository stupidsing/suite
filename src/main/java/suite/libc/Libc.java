package suite.libc;

import java.nio.ByteBuffer;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface Libc extends Library {

	public static Libc instance = (Libc) Native.loadLibrary("c", Libc.class);

	public int getchar();

	public int ioctl(int d, int request, ByteBuffer data);

	public int tcgetattr(int fd, ByteBuffer termios_p);

	public int tcsetattr(int fd, int optional_actions, ByteBuffer termios_p);

	public int cfmakeraw(ByteBuffer termios_p);

}
