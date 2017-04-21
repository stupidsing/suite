package suite.ansi;

import com.sun.jna.Library;

public interface LibcJna extends Library {

	public int cfmakeraw(byte termios_p[]);

	public int getchar();

	public int ioctl(int d, int request, byte[] data);

	public int putchar(int ch);

	public int tcgetattr(int fd, byte termios_p[]);

	public int tcsetattr(int fd, int optional_actions, byte termios_p[]);

}
