package suite.dev;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.Native;

import suite.adt.pair.Fixie_.FixieFun5;
import suite.ansi.Keyboard;
import suite.ansi.Keyboard.VK;
import suite.ansi.LibcJna;
import suite.ansi.Termios;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil.Sink;
import suite.util.Rethrow;

// mvn compile exec:java -Dexec.mainClass=suite.dev.DevMain
public class DevMain {

	private LibcJna libc = (LibcJna) Native.loadLibrary("c", LibcJna.class);

	public static void main(String[] args) {
		new DevMain().run();
	}

	private void run() {
		List<String> input = Rethrow.ex(() -> Files.readAllLines(Paths.get("src/main/java/suite/dev/DevMain.java")));
		int screenSizeX = Integer.valueOf(System.getenv("COLUMNS"));
		int screenSizeY = Integer.valueOf(System.getenv("LINES"));

		char[] cs = new char[screenSizeX];
		Arrays.fill(cs, ' ');
		String blankY = new String(cs);

		try (Termios termios = new Termios(libc);) {
			Keyboard keyboard = new Keyboard(libc);

			Sink<State> redraw = state -> {
				termios.clear();
				termios.cursor(false);

				List<String> lines_ = state.lines;

				for (int screenY = 0; screenY < screenSizeY; screenY++) {
					int y = screenY - state.offsetY;
					String line = 0 <= y && y < lines_.size() ? lines_.get(y) : "";
					String s = (line.substring(state.offsetX) + blankY).substring(0, screenSizeX);

					termios.gotoxy(0, screenY);
					termios.puts(s);
				}

				termios.cursor(true);
				termios.gotoxy(state.cursorX - state.offsetX, state.cursorY - state.offsetY);
			};

			keyboard //
					.signal() //
					.map(pair -> pair.map((vk, c) -> {
						Iterate<State> mutate = state -> state.apply((lines, offsetX, offsetY, cursorX, cursorY) -> {
							if (vk == VK.LEFT_)
								return new State(lines, offsetX, offsetY, cursorX - 1, cursorY);
							else if (vk == VK.RIGHT)
								return new State(lines, offsetX, offsetY, cursorX + 1, cursorY);
							else if (vk == VK.UP___)
								return new State(lines, offsetX, offsetY, cursorX, cursorY - 1);
							else if (vk == VK.DOWN_)
								return new State(lines, offsetX, offsetY, cursorX, cursorY + 1);
							else
								return new State(lines, offsetX, offsetY, cursorX, cursorY);
						});
						return mutate;
					})) //
					.fold(new State(input, 0, 0, 0, 0), (state, mutate) -> mutate.apply(state)) //
					.wire(redraw);
		}
	}

	private class State {
		public final List<String> lines;
		public final int offsetX, offsetY;
		public final int cursorX, cursorY;

		public State(List<String> lines, int offsetX, int offsetY, int cursorX, int cursorY) {
			this.lines = lines;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.cursorX = cursorX;
			this.cursorY = cursorY;
		}

		public <R> R apply(FixieFun5<List<String>, Integer, Integer, Integer, Integer, R> fun) {
			return fun.apply(lines, offsetX, offsetY, cursorX, cursorY);
		}
	}

}
