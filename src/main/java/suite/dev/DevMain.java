package suite.dev;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.sun.jna.Native;

import suite.adt.pair.Fixie_.FixieFun3;
import suite.ansi.Keyboard;
import suite.ansi.Keyboard.VK;
import suite.ansi.LibcJna;
import suite.ansi.Termios;
import suite.primitive.Chars_;
import suite.primitive.Int_Int;
import suite.primitive.Ints_;
import suite.primitive.adt.pair.IntIntPair;
import suite.util.Fail;
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
		int viewSizeX = screenSizeX;
		int viewSizeY = screenSizeY - 1;
		List<String> input = Rethrow.ex(() -> Files.readAllLines(Paths.get("src/main/java/suite/dev/DevMain.java")));

		try (Termios termios = new Termios(libc);) {
			termios.clear();
			Keyboard keyboard = new Keyboard(libc);

			Sink<State> redraw = state -> state.apply((text, oc, cc) -> cc.apply((cx, cy) -> oc.apply((ox, oy) -> {
				termios.cursor(false);

				for (int screenY = 0; screenY < viewSizeY; screenY++) {
					termios.gotoxy(0, screenY);
					termios.puts(text.get(ox, oy + screenY, viewSizeX).replace('\t', ' '));
				}

				termios.gotoxy(cx - ox, cy - oy);
				termios.cursor(true);
				return null;
			})));

			State state0 = State.of(new Text(input), IntIntPair.of(0, 0), IntIntPair.of(0, 0));
			redraw.sink(state0);

			FixieFun3<VK, Character, State, State> mutate = (vk, ch, state) -> state //
					.apply((text, oc, cc) -> cc.apply((cx, cy) -> {
						if (vk == VK.LEFT_)
							return State.of(text, oc, IntIntPair.of(cx - 1, cy));
						else if (vk == VK.RIGHT)
							return State.of(text, oc, IntIntPair.of(cx + 1, cy));
						else if (vk == VK.UP___)
							return State.of(text, oc, IntIntPair.of(cx, cy - 1));
						else if (vk == VK.DOWN_)
							return State.of(text, oc, IntIntPair.of(cx, cy + 1));
						else if (vk == VK.PGUP_)
							return State.of(text, oc, IntIntPair.of(cx, cy - viewSizeY));
						else if (vk == VK.PGDN_)
							return State.of(text, oc, IntIntPair.of(cx, cy + viewSizeY));
						else if (vk == VK.HOME_)
							return State.of(text, oc, IntIntPair.of(0, cy));
						else if (vk == VK.END__) {
							int index = text.index(0, cy + 1);
							return 0 < index ? State.of(text, oc, text.coord(index - 1)) : state;
						} else if (vk == VK.BKSP_) {
							int index = text.index(cx, cy);
							if (0 < index) {
								IntIntPair cc1 = text.coord(index - 1);
								return State.of(text.splice(cc1.t0, cc1.t1, 1, ""), oc, cc1);
							} else
								return state;
						} else if (ch != null)
							if (ch != 'q')
								return State.of(text.splice(cx, cy, 0, Character.toString(ch)), oc, IntIntPair.of(cx + 1, cy));
							else
								return Fail.t();
						else
							return state;
					})).apply((text, oc, cc) -> oc.apply((ox, oy) -> cc.apply((cx, cy) -> {
						int cx_ = Math.max(0, cx);
						int cy_ = Math.max(0, Math.min(text.lines().length, cy));
						return State.of(text, oc, IntIntPair.of(cx_, cy_));
					}))).apply((text, oc, cc) -> oc.apply((ox, oy) -> cc.apply((cx, cy) -> {
						int ox_ = Math.max(cx - viewSizeX + 1, Math.min(cx, ox));
						int oy_ = Math.max(cy - viewSizeY + 1, Math.min(cy, oy));
						return State.of(text, IntIntPair.of(ox_, oy_), cc);
					})));

			keyboard.loop(signal -> signal //
					.fold(state0, (state, pair_) -> pair_.map((vk, ch) -> mutate.apply(vk, ch, state))) //
					.wire(redraw));
		}
	}

	private static class State {
		public Text text;
		public IntIntPair offsetCoord;
		public IntIntPair cursorCoord;

		public static State of(Text text, IntIntPair offsetCoord, IntIntPair cursorCoord) {
			State s = new State();
			s.text = text;
			s.offsetCoord = offsetCoord;
			s.cursorCoord = cursorCoord;
			return s;
		}

		public <R> R apply(FixieFun3<Text, IntIntPair, IntIntPair, R> fun) {
			return fun.apply(text, offsetCoord, cursorCoord);
		}
	}

	private static class Text {
		public String text;
		public String[] lines;

		private Text(String text) {
			this(text, text.split("\n"));
		}

		private Text(List<String> lines) {
			this(String.join("\n", lines), lines.toArray(new String[0]));
		}

		private Text(String text, String[] lines) {
			this.text = text;
			this.lines = lines;
		}

		private String get(int px, int py, int length) {
			String line = line(py);

			return new String(Chars_.toArray(length, x -> {
				int x_ = x + px;
				return x_ < line.length() ? line.charAt(x) : ' ';
			}));
		}

		private Text splice(int px, int py, int deletes, String s) {
			if (Boolean.TRUE) {
				int length = text.length();
				int i0 = index(px, py);
				int i1 = Math.min(length, i0 + deletes);
				return new Text(text.substring(0, i0) + s + text.substring(i1, length));
			} else {
				String[] lines0 = lines();

				List<String> lines1 = Ints_ //
						.range(lines0.length) //
						.map(y -> {
							String line = lines0[y];
							if (py != y)
								return line;
							else {
								char[] cs_ = Chars_.toArray(px, i -> i < line.length() ? line.charAt(i) : ' ');
								String s0 = new String(cs_);
								String sx = 0 <= px && px < line.length() ? line.substring(px) : "";
								return s0 + s + sx;
							}
						}) //
						.toList();

				return new Text(lines1);
			}
		}

		private String line(int py) {
			String[] lines = lines();
			return py < lines.length ? lines[py] : "";
		}

		private int index(int px, int py) {
			String[] lines = lines();
			if (py < lines.length)
				return Ints_.range(py).toInt(Int_Int.sum(y -> lines[y].length() + 1)) + Math.min(lines[py].length(), px);
			else
				return text.length();
		}

		private IntIntPair coord(int index) {
			String[] lines = lines();
			int index1;
			int y = 0;
			while (y < lines.length && 0 <= (index1 = index - (lines[y].length() + 1))) {
				index = index1;
				y++;
			}
			return IntIntPair.of(index, y);
		}

		private String[] lines() {
			return lines;
		}
	}

}
