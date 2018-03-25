package suite.dev;

import static suite.util.Friends.max;
import static suite.util.Friends.min;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.Native;

import suite.adt.pair.Fixie_.FixieFun3;
import suite.adt.pair.Fixie_.FixieFun6;
import suite.ansi.Keyboard;
import suite.ansi.Keyboard.VK;
import suite.ansi.LibcJna;
import suite.ansi.Termios;
import suite.primitive.Chars_;
import suite.primitive.Ints_;
import suite.primitive.adt.pair.IntIntPair;
import suite.util.Fail;
import suite.util.FunUtil.Sink;
import suite.util.Rethrow;

// mvn compile exec:java -Dexec.mainClass=suite.dev.DevMain -Dexec.args="${COLUMNS} ${LINES}"
public class DevMain {

	private LibcJna libc = (LibcJna) Native.loadLibrary("c", LibcJna.class);

	private int viewSizeX;
	private int viewSizeY;

	public static void main(String[] args) {
		int screenSizeX = Integer.valueOf(args[0]); // Integer.valueOf(System.getenv("COLUMNS"));
		int screenSizeY = Integer.valueOf(args[1]); // Integer.valueOf(System.getenv("LINES"));
		new DevMain(screenSizeX, screenSizeY).run();
	}

	private DevMain(int screenSizeX, int screenSizeY) {
		viewSizeX = screenSizeX;
		viewSizeY = screenSizeY - 1;
	}

	private void run() {
		List<String> input = Rethrow.ex(() -> Files.readAllLines(Paths.get("src/main/java/suite/dev/DevMain.java")));

		try (Termios termios = new Termios(libc);) {
			termios.clear();
			Keyboard keyboard = new Keyboard(libc);

			Sink<State> redraw = state -> state.apply((st, prev, next, text, oc, cc) -> cc.apply((cx, cy) -> oc.apply((ox, oy) -> {
				String[] lines = Ints_ //
						.range(viewSizeY) //
						.map(screenY -> text.get(ox, oy + screenY, viewSizeX).replace('\t', ' ')) //
						.toArray(String.class);

				termios.cursor(false);

				for (int screenY = 0; screenY < viewSizeY; screenY++) {
					termios.gotoxy(0, screenY);
					termios.puts(lines[screenY]);
				}

				termios.gotoxy(cx - ox, cy - oy);
				termios.cursor(true);
				return null;
			})));

			State state0 = new State(null, null, text(input), c(0, 0), c(0, 0));
			redraw.sink(state0);

			FixieFun3<VK, Character, State, State> mutate = (vk, ch, state) -> state //
					.apply((st, prev, next, text, oc, cc) -> oc.apply((ox, oy) -> cc.apply((cx, cy) -> {
						if (vk == VK.LEFT_)
							return st.cursorCoord(c(cx - 1, cy));
						else if (vk == VK.RIGHT)
							return st.cursorCoord(c(cx + 1, cy));
						else if (vk == VK.UP___)
							return st.cursorCoord(c(cx, cy - 1));
						else if (vk == VK.DOWN_)
							return st.cursorCoord(c(cx, cy + 1));
						else if (vk == VK.PGUP_)
							return st.cursorCoord(c(cx, cy - viewSizeY));
						else if (vk == VK.PGDN_)
							return st.cursorCoord(c(cx, cy + viewSizeY));
						else if (vk == VK.HOME_)
							return st.cursorCoord(c(0, cy));
						else if (vk == VK.END__)
							return st.cursorCoord(text.coord(text.end(cy)));
						else if (vk == VK.CTRL_HOME_)
							return st.cursorCoord(text.coord(0));
						else if (vk == VK.CTRL_END__)
							return st.cursorCoord(text.coord(text.length()));
						else if (vk == VK.CTRL_LEFT_) {
							int index = text.index(cx, cy), index1;
							while (0 <= (index1 = index - 1) && Character.isJavaIdentifierPart(text.at(index = index1)))
								;
							return st.cursorCoord(text.coord(index));
						} else if (vk == VK.CTRL_RIGHT) {
							int index = text.index(cx, cy), index1;
							while ((index1 = index + 1) < text.length() && Character.isJavaIdentifierPart(text.at(index = index1)))
								;
							return st.cursorCoord(text.coord(index));
						} else if (vk == VK.CTRL_UP___) {
							int oy1 = max(cy - viewSizeY + 1, 0);
							if (oy != oy1)
								return st.offsetCoord(c(ox, oy1));
							else
								return st.offsetCoord(c(ox, oy - viewSizeY)).cursorCoord(c(cx, cy - viewSizeY));
						} else if (vk == VK.CTRL_DOWN_) {
							int oy1 = min(cy, text.nLines());
							if (oy != oy1)
								return st.offsetCoord(c(ox, oy1));
							else
								return st.offsetCoord(c(ox, oy + viewSizeY)).cursorCoord(c(cx, cy + viewSizeY));
						} else if (vk == VK.ALT_J____) {
							int index = text.index(cx, cy);
							while (index < text.length() && text.at(index) != '\n')
								index++;
							Text text1 = text.splice(index, index + 1, "");
							return st.text(text1).cursorCoord(text1.coord(index));
						} else if (vk == VK.BKSP_) {
							int index = text.index(cx, cy);
							return 0 < index ? st.splice(index - 1, index, "") : st;
						} else if (vk == VK.DEL__)
							return st.splice(1, "");
						else if (vk == VK.CTRL_K____)
							return st.splice(text.index(cx, cy), text.end(cy), "");
						else if (vk == VK.CTRL_U____)
							return st.splice(text.start(cy), text.index(cx, cy), "");
						else if (vk == VK.CTRL_D____)
							return st.splice(text.index(0, cy), text.index(0, cy + 1), "");
						else if (vk == VK.CTRL_Y____)
							return next != null ? next : st;
						else if (vk == VK.CTRL_Z____) {
							State prev1 = prev != null ? prev : st;
							return new State(prev1.prev, st, prev1.text, oc, prev1.cursorCoord);
						} else if (vk == VK.CTRL_C____)
							return Fail.t();
						else if (ch != null)
							if (ch == 13) {
								int i0 = text.index(0, cy);
								int ix = i0;
								char ch_;
								while ((ch_ = text.at(ix)) == ' ' || ch_ == '\t')
									ix++;
								return st.splice(0, "\n" + text.text.substring(i0, ix));
							} else
								return st.splice(0, Character.toString(ch));
						else
							return st;
					}))).apply((st, prev, next, text, oc, cc) -> oc.apply((ox, oy) -> cc.apply((cx, cy) -> {
						return st.cursorCoord(text.coord(text.index(cc.t0, cc.t1)));
					}))).apply((st, prev, next, text, oc, cc) -> oc.apply((ox, oy) -> cc.apply((cx, cy) -> {
						int ox_ = sat(ox, cx - viewSizeX + 1, cx);
						int oy_ = sat(oy, cy - viewSizeY + 1, cy);
						return st.offsetCoord(c(ox_, oy_));
					})));

			keyboard.loop(signal -> signal //
					.fold(state0, (state, pair_) -> pair_.map((vk, ch) -> mutate.apply(vk, ch, state))) //
					.wire(redraw));
		}
	}

	private class State {
		private State prev;
		private State next;
		private Text text;
		private IntIntPair offsetCoord;
		private IntIntPair cursorCoord;

		private State(State prev, State next, Text text, IntIntPair offsetCoord, IntIntPair cursorCoord) {
			this.prev = prev;
			this.next = next;
			this.text = text;
			this.offsetCoord = offsetCoord;
			this.cursorCoord = cursorCoord;
		}

		private State splice(int deletes, String s) {
			int index = text.index(cursorCoord.t0, cursorCoord.t1);
			return splice(index, index + deletes, s);
		}

		private State splice(int i0, int ix, String s) {
			int cursorIndex0 = text.index(cursorCoord.t0, cursorCoord.t1);
			int cursorIndex1;
			if (cursorIndex0 < i0)
				cursorIndex1 = cursorIndex0;
			else if (cursorIndex0 < ix)
				cursorIndex1 = i0;
			else
				cursorIndex1 = cursorIndex0 - ix + i0 + s.length();
			Text text1 = text.splice(i0, ix, s);
			return text(text1).cursorCoord(text1.coord(cursorIndex1));
		}

		private State text(Text text) {
			State state = this, state1;
			for (int i = 0; i < 16 && (state1 = state.prev) != null; i++)
				state = state1;
			if (state != null)
				state.prev = null;
			return new State(this, null, text, offsetCoord, cursorCoord);
		}

		private State offsetCoord(IntIntPair offsetCoord) {
			return new State(prev, next, text, offsetCoord, cursorCoord);
		}

		private State cursorCoord(IntIntPair cursorCoord) {
			return new State(prev, next, text, offsetCoord, cursorCoord);
		}

		private <R> R apply(FixieFun6<State, State, State, Text, IntIntPair, IntIntPair, R> fun) {
			return fun.apply(this, prev, next, text, offsetCoord, cursorCoord);
		}
	}

	private Text text(String text) {
		return text(Arrays.asList(text.split("\n")));
	}

	private Text text(List<String> lines) {
		int size = lines.size();
		int[] starts = new int[size];
		int[] ends = new int[size];
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < size; i++) {
			starts[i] = sb.length();
			sb.append(lines.get(i));
			ends[i] = sb.length();
			sb.append("\n");
		}

		return new Text(sb.toString(), starts, ends);
	}

	private class Text {
		private String text;
		private int[] starts;
		private int[] ends;

		private Text(String text, int[] starts, int[] ends) {
			this.text = text;
			this.starts = starts;
			this.ends = ends;
		}

		private String get(int px, int py, int length) {
			int i0 = start(py) + px;
			int ix = end(py);
			return new String(Chars_.toArray(length, i_ -> {
				int i = i_ + i0;
				return i < ix ? text.charAt(i) : ' ';
			}));
		}

		private Text splice(int i0, int i1, String s) {
			int length = length();
			int i1_ = min(i1, length);
			return text(text.substring(0, i0) + s + text.substring(i1_, length));
		}

		private int index(int px, int py) {
			return min(start(py) + px, end(py));
		}

		private IntIntPair coord(int index) {
			int nLines = nLines();
			int y = 0, y1;
			while ((y1 = y + 1) <= nLines && start(y1) <= index)
				y = y1;
			return c(index - start(y), y);
		}

		private int start(int y) {
			return y < nLines() ? starts[y] : length();
		}

		private int end(int y) {
			return y < nLines() ? ends[y] : length();
		}

		private int nLines() {
			return starts.length;
		}

		private char at(int arg0) {
			return text.charAt(arg0);
		}

		private int length() {
			return text.length();
		}
	}

	private static IntIntPair c(int x, int y) {
		return IntIntPair.of(x, y);
	}

	private static int sat(int x, int min, int max) {
		return min(max(x, min), max);
	}

}
