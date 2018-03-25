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
import suite.immutable.IRope.IRopeList;
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
							return st.cursorCoord(cx - 1, cy);
						else if (vk == VK.RIGHT)
							return st.cursorCoord(cx + 1, cy);
						else if (vk == VK.UP___)
							return st.cursorCoord(cx, cy - 1);
						else if (vk == VK.DOWN_)
							return st.cursorCoord(cx, cy + 1);
						else if (vk == VK.PGUP_)
							return st.cursorCoord(cx, cy - viewSizeY);
						else if (vk == VK.PGDN_)
							return st.cursorCoord(cx, cy + viewSizeY);
						else if (vk == VK.HOME_)
							return st.cursorCoord(0, cy);
						else if (vk == VK.END__)
							return st.cursor(text.end(cy));
						else if (vk == VK.CTRL_HOME_)
							return st.cursor(0);
						else if (vk == VK.CTRL_END__)
							return st.cursor(text.length());
						else if (vk == VK.CTRL_LEFT_) {
							int index = text.index(cx, cy), index1;
							while (0 <= (index1 = index - 1) && Character.isJavaIdentifierPart(text.at(index = index1)))
								;
							return st.cursor(index);
						} else if (vk == VK.CTRL_RIGHT) {
							int index = text.index(cx, cy), index1;
							while ((index1 = index + 1) < text.length() && Character.isJavaIdentifierPart(text.at(index = index1)))
								;
							return st.cursor(index);
						} else if (vk == VK.CTRL_UP___) {
							int oy1 = max(cy - viewSizeY + 1, 0);
							if (oy != oy1)
								return st.offsetCoord(ox, oy1);
							else
								return st.offsetCoord(ox, oy - viewSizeY).cursorCoord(cx, cy - viewSizeY);
						} else if (vk == VK.CTRL_DOWN_) {
							int oy1 = min(cy, text.nLines());
							if (oy != oy1)
								return st.offsetCoord(ox, oy1);
							else
								return st.offsetCoord(ox, oy + viewSizeY).cursorCoord(cx, cy + viewSizeY);
						} else if (vk == VK.ALT_J____) {
							int index = text.index(cx, cy);
							while (index < text.length() && text.at(index) != '\n')
								index++;
							Text text1 = text.splice(index, index + 1, empty);
							return st.text(text1).cursor(index);
						} else if (vk == VK.BKSP_) {
							int index = text.index(cx, cy);
							return 0 < index ? st.splice(index - 1, index, empty) : st;
						} else if (vk == VK.ALT_UP___)
							if (0 < cy) {
								int i0 = text.start(cy - 1);
								int i1 = text.start(cy);
								int i2 = text.start(cy + 1);
								return st.splice(i2, i2, text.text.subList(i0, i1)).splice(i0, i1, empty);
							} else
								return st;
						else if (vk == VK.ALT_DOWN_)
							if (cy < text.length()) {
								int i0 = text.start(cy);
								int i1 = text.start(cy + 1);
								int i2 = text.start(cy + 2);
								return st.splice(i1, i2, empty).splice(i0, i0, text.text.subList(i1, i2));
							} else
								return st;
						else if (vk == VK.DEL__)
							return st.splice(1, empty);
						else if (vk == VK.CTRL_K____)
							return st.splice(text.index(cx, cy), text.end(cy), empty);
						else if (vk == VK.CTRL_U____)
							return st.splice(text.start(cy), text.index(cx, cy), empty);
						else if (vk == VK.CTRL_D____)
							return st.splice(text.index(0, cy), text.index(0, cy + 1), empty);
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
								return st.splice(0, ropeList("\n").concat(text.text.subList(i0, ix)));
							} else
								return st.splice(0, ropeList(Character.toString(ch)));
						else
							return st;
					}))).apply((st, prev, next, text, oc, cc) -> oc.apply((ox, oy) -> cc.apply((cx, cy) -> {
						return st.cursor(text.index(cc.t0, cc.t1));
					}))).apply((st, prev, next, text, oc, cc) -> oc.apply((ox, oy) -> cc.apply((cx, cy) -> {
						int ox_ = sat(ox, cx - viewSizeX + 1, cx);
						int oy_ = sat(oy, cy - viewSizeY + 1, cy);
						return st.offsetCoord(ox_, oy_);
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

		private State splice(int deletes, IRopeList<Character> s) {
			int index = text.index(cursorCoord.t0, cursorCoord.t1);
			return splice(index, index + deletes, s);
		}

		private State splice(int i0, int ix, IRopeList<Character> s) {
			int cursorIndex0 = text.index(cursorCoord.t0, cursorCoord.t1);
			int cursorIndex1;
			if (cursorIndex0 < i0)
				cursorIndex1 = cursorIndex0;
			else if (cursorIndex0 < ix)
				cursorIndex1 = i0;
			else
				cursorIndex1 = cursorIndex0 - ix + i0 + s.size();
			Text text1 = text.splice(i0, ix, s);
			return text(text1).cursor(cursorIndex1);
		}

		private State text(Text text) {
			State state = this, state1;
			for (int i = 0; i < 16 && (state1 = state.prev) != null; i++)
				state = state1;
			if (state != null)
				state.prev = null;
			return new State(this, null, text, offsetCoord, cursorCoord);
		}

		private State offsetCoord(int ox, int oy) {
			return new State(prev, next, text, c(ox, oy), cursorCoord);
		}

		private State cursor(int index) {
			IntIntPair coord = text.coord(index);
			return cursorCoord(coord.t0, coord.t1);
		}

		private State cursorCoord(int cx, int cy) {
			return new State(prev, next, text, offsetCoord, c(cx, cy));
		}

		private <R> R apply(FixieFun6<State, State, State, Text, IntIntPair, IntIntPair, R> fun) {
			return fun.apply(this, prev, next, text, offsetCoord, cursorCoord);
		}
	}

	private Text text(IRopeList<Character> text) {
		return text(Arrays.asList(text.toString().split("\n")));
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

		return new Text(ropeList(sb.toString()), starts, ends);
	}

	private class Text {
		private IRopeList<Character> text;
		private int[] starts;
		private int[] ends;

		private Text(IRopeList<Character> text, int[] starts, int[] ends) {
			this.text = text;
			this.starts = starts;
			this.ends = ends;
		}

		private String get(int px, int py, int length) {
			int i0 = start(py) + px;
			int ix = end(py);
			return new String(Chars_.toArray(length, i_ -> {
				int i = i_ + i0;
				return i < ix ? text.get(i) : ' ';
			}));
		}

		private Text splice(int i0, int i1, IRopeList<Character> s) {
			int i1_ = min(i1, length());
			return text(text.subList(0, i0).concat(s.concat(text.subList(i1_, 0))));
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

		private char at(int index) {
			return text.get(index);
		}

		private int length() {
			return text.size();
		}
	}

	private IRopeList<Character> empty = ropeList("");

	private IRopeList<Character> ropeList(String s) {
		int size = s.length();

		return new IRopeList<>() {
			public int size() {
				return size;
			}

			public Character get(int index) {
				return s.charAt(index);
			}

			public IRopeList<Character> subList(int i0, int ix) {
				int s_ = i0 + (i0 < 0 ? size : 0);
				int e_ = ix + (ix <= 0 ? size : 0);
				return ropeList(s.substring(s_, e_));
			}

			public IRopeList<Character> concat(IRopeList<Character> list) {
				return ropeList(s + list.toString());
			}

			public String toString() {
				return s;
			}
		};
	}

	private static IntIntPair c(int x, int y) {
		return IntIntPair.of(x, y);
	}

	private static int sat(int x, int min, int max) {
		return min(max(x, min), max);
	}

}
