package suite.ansi;

import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.ann.Library;

@Library("c")
public class LibcBridj {

	static {
		BridJ.register();
	}

	public static native int cfmakeraw(Pointer<Byte> termios_p);

	public static native int getchar();

	public static native int ioctl(int d, int request, Pointer<Byte> data);

	public static native int putchar(int ch);

	public static native int tcgetattr(int fd, Pointer<Byte> termios_p);

	public static native int tcsetattr(int fd, int optional_actions, Pointer<Byte> termios_p);

}
