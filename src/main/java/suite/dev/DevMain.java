package suite.dev;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.Native;

import suite.adt.pair.Fixie_.FixieFun3;
import suite.ansi.Keyboard;
import suite.ansi.Keyboard.VK;
import suite.ansi.LibcJna;
import suite.ansi.Termios;
import suite.primitive.Chars_;
import suite.primitive.Ints_;
import suite.primitive.adt.pair.IntIntPair;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil.Sink;
import suite.util.Rethrow;

// mvn compile exec:java -Dexec.mainClass=suite.dev.DevMain -Dexec.args="${COLUMNS} ${LINES}"
public class DevMain {

	private LibcJna libc = (LibcJna) Native.loadLibrary("c", LibcJna.class);

	public static void main(String[] args) {
		int screenSizeX = Integer.valueOf(args[0]); // Integer.valueOf(System.getenv("COLUMNS"));
		int screenSizeY = Integer.valueOf(args[1]); // Integer.valueOf(System.getenv("LINES"));
		new DevMain().run(screenSizeX, screenSizeY);
	}

	private void run(int screenSizeX, int screenSizeY) {
		List<String> input = Rethrow.ex(() -> Files.readAllLines(Paths.get("src/main/java/suite/dev/DevMain.java")));

		char[] cs = new char[screenSizeX];
		Arrays.fill(cs, ' ');
		String blankY = new String(cs);

		try (Termios termios = new Termios(libc);) {
			Keyboard keyboard = new Keyboard(libc);

			Sink<State> redraw = state -> state.apply((lines, oc, cc) -> cc.apply((cx, cy) -> oc.apply((ox, oy) -> {
				termios.clear();
				termios.cursor(false);

				for (int screenY = 0; screenY < screenSizeY - 1; screenY++) {
					int y = screenY - oy;
					String line = 0 <= y && y < lines.size() ? lines.get(y) : "";
					String s = (line.substring(ox) + blankY).substring(0, screenSizeX);

					termios.gotoxy(0, screenY);
					termios.puts(s);
				}

				termios.gotoxy(cx - ox, cy - oy);
				termios.cursor(true);
				return null;
			})));

			State state0 = State.of(input, IntIntPair.of(0, 0), IntIntPair.of(0, 0));
			redraw.sink(state0);

			keyboard //
					.signal() //
					.map(pair -> pair.map((vk, ch) -> {
						Iterate<State> mutate = state -> state.apply((lines, oc, cc) -> cc.apply((cx, cy) -> {
							if (vk == VK.LEFT_)
								return State.of(lines, oc, IntIntPair.of(cx - 1, cy));
							else if (vk == VK.RIGHT)
								return State.of(lines, oc, IntIntPair.of(cx + 1, cy));
							else if (vk == VK.UP___)
								return State.of(lines, oc, IntIntPair.of(cx, cy - 1));
							else if (vk == VK.DOWN_)
								return State.of(lines, oc, IntIntPair.of(cx, cy + 1));
							else if (ch != null) {
								List<String> lines1 = Ints_ //
										.range(lines.size()) //
										.map(y -> {
											String line = lines.get(y);
											if (cy != y)
												return line;
											else {
												char[] cs_ = Chars_.toArray(cx, i -> i < line.length() ? line.charAt(i) : ' ');
												String s0 = new String(cs_);
												String sx = line.substring(cx);
												return s0 + ch + sx;
											}
										}) //
										.toList();
								return State.of(lines1, oc, IntIntPair.of(cx + 1, cy));
							} else
								return state;
						}));
						return mutate;
					})) //
					.fold(state0, (state, mutate) -> mutate.apply(state)) //
					.wire(redraw);
		}
	}

	private static class State {
		public List<String> lines;
		public IntIntPair offsetCoord;
		public IntIntPair cursorCoord;

		public static State of(List<String> lines, IntIntPair offsetCoord, IntIntPair cursorCoord) {
			State s = new State();
			s.lines = lines;
			s.offsetCoord = offsetCoord;
			s.cursorCoord = cursorCoord;
			return s;
		}

		public <R> R apply(FixieFun3<List<String>, IntIntPair, IntIntPair, R> fun) {
			return fun.apply(lines, offsetCoord, cursorCoord);
		}
	}

}
