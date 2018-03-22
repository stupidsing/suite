package suite.dev;

import static suite.util.Friends.max;
import static suite.util.Friends.min;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.sun.jna.Native;

import suite.adt.pair.Fixie_.FixieFun3;
import suite.adt.pair.Fixie_.FixieFun5;
import suite.ansi.Keyboard;
import suite.ansi.Keyboard.VK;
import suite.ansi.LibcJna;
import suite.ansi.Termios;
import suite.primitive.Chars_;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Int;
import suite.primitive.Ints_;
import suite.primitive.adt.pair.IntIntPair;
import suite.streamlet.Read;
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

			Sink<State> redraw = state -> state.apply((st, prev, text, oc, cc) -> cc.apply((cx, cy) -> oc.apply((ox, oy) -> {
				termios.cursor(false);

				for (int screenY = 0; screenY < viewSizeY; screenY++) {
					termios.gotoxy(0, screenY);
					termios.puts(text.get(ox, oy + screenY, viewSizeX).replace('\t', ' '));
				}

				termios.gotoxy(cx - ox, cy - oy);
				termios.cursor(true);
				return null;
			})));

			State state0 = new State(null, new Text(input), c(0, 0), c(0, 0));
			redraw.sink(state0);

			FixieFun3<VK, Character, State, State> mutate = (vk, ch, state) -> state //
					.apply((st, prev, text, oc, cc) -> oc.apply((ox, oy) -> cc.apply((cx, cy) -> {
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
						else if (vk == VK.END__) {
							int index = text.index(0, cy + 1);
							return 0 < index ? st.cursorCoord(text.coord(index - 1)) : st;
						} else if (vk == VK.CTRL_LEFT_) {
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
							int oy1 = max(0, cy - viewSizeY + 1);
							if (oy != oy1)
								return st.offsetCoord(c(ox, oy1));
							else
								return st.text(text).offsetCoord(c(ox, oy - viewSizeY)).cursorCoord(c(cx, cy - viewSizeY));
						} else if (vk == VK.CTRL_DOWN_) {
							int oy1 = min(text.lineLengths().length, cy);
							if (oy != oy1)
								return st.offsetCoord(c(ox, oy1));
							else
								return st.text(text).offsetCoord(c(ox, oy + viewSizeY)).cursorCoord(c(cx, cy + viewSizeY));
						} else if (vk == VK.ALT_J____) {
							int cy1 = cy + 1;
							int index = text.index(0, cy1) - 1;
							Text text1 = text.splice(index, 1, "");
							return st.text(text1).cursorCoord(text1.coord(text1.index(0, cy1) - 1));
						} else if (vk == VK.BKSP_) {
							int index = text.index(cx, cy);
							if (0 < index) {
								IntIntPair cc1 = text.coord(index - 1);
								return st.text(text.splice(cc1.t0, cc1.t1, 1, "")).cursorCoord(cc1);
							} else
								return st;
						} else if (vk == VK.DEL__)
							return st.text(text.splice(cx, cy, 1, ""));
						else if (ch != null)
							if (ch == 13)
								return st.text(text.splice(cx, cy, 0, "\n")).cursorCoord(c(0, cy + 1));
							else if (ch == 26) { // ctrl-Z
								State parent0 = st.previous;
								State parent1 = parent0 != null ? parent0 : st;
								return new State(parent1.previous, parent1.text, oc, parent1.cursorCoord);
							} else if (ch == 'q')
								return Fail.t();
							else
								return st.text(text.splice(cx, cy, 0, Character.toString(ch))).cursorCoord(c(cx + 1, cy));
						else
							return st;
					}))).apply((st, prev, text, oc, cc) -> oc.apply((ox, oy) -> cc.apply((cx, cy) -> {
						int cx_ = max(0, cx);
						int cy_ = max(0, min(text.lineLengths().length, cy));
						return st.cursorCoord(c(cx_, cy_));
					}))).apply((st, prev, text, oc, cc) -> oc.apply((ox, oy) -> cc.apply((cx, cy) -> {
						int ox_ = max(0, max(cx - viewSizeX + 1, min(cx, ox)));
						int oy_ = max(0, max(cy - viewSizeY + 1, min(cy, oy)));
						return st.offsetCoord(c(ox_, oy_));
					})));

			keyboard.loop(signal -> signal //
					.fold(state0, (state, pair_) -> pair_.map((vk, ch) -> mutate.apply(vk, ch, state))) //
					.wire(redraw));
		}
	}

	private static class State {
		public State previous;
		public Text text;
		public IntIntPair offsetCoord;
		public IntIntPair cursorCoord;

		public State(State previous, Text text, IntIntPair offsetCoord, IntIntPair cursorCoord) {
			this.previous = previous;
			this.text = text;
			this.offsetCoord = offsetCoord;
			this.cursorCoord = cursorCoord;
		}

		public State text(Text text) {
			State state = this;
			for (int i = 0; i < 16; i++)
				if (state != null)
					state = state.previous;
				else
					break;
			if (state != null)
				state.previous = null;
			return new State(this, text, offsetCoord, cursorCoord);
		}

		public State offsetCoord(IntIntPair offsetCoord) {
			return new State(previous, text, offsetCoord, cursorCoord);
		}

		public State cursorCoord(IntIntPair cursorCoord) {
			return new State(previous, text, offsetCoord, cursorCoord);
		}

		public <R> R apply(FixieFun5<State, State, Text, IntIntPair, IntIntPair, R> fun) {
			return fun.apply(this, previous, text, offsetCoord, cursorCoord);
		}
	}

	private static class Text {
		public String text;
		public int[] lineLengths;

		private Text(String text) {
			this(text, text.split("\n"));
		}

		private Text(List<String> lines) {
			this(String.join("\n", lines), lines.toArray(new String[0]));
		}

		private Text(String text, String[] lines) {
			this.text = text;
			this.lineLengths = Read.from(lines).collect(Obj_Int.lift(String::length)).toArray();
		}

		private String get(int px, int py, int length) {
			int i0 = index(px, py);
			int ix = index(0, py + 1) - 1;
			return new String(Chars_.toArray(length, i_ -> {
				int i = i_ + i0;
				return i < ix ? text.charAt(i) : ' ';
			}));
		}

		private Text splice(int px, int py, int deletes, String s) {
			return splice(index(px, py), deletes, s);
		}

		private Text splice(int index, int deletes, String s) {
			int length = text.length();
			int i1 = min(length, index + deletes);
			return new Text(text.substring(0, index) + s + text.substring(i1, length));
		}

		private int index(int px, int py) {
			int[] lineLengths = lineLengths();
			if (py < lineLengths.length)
				return Ints_.range(py).toInt(Int_Int.sum(y -> lineLengths[y] + 1)) + min(lineLengths[py], px);
			else
				return text.length();
		}

		private IntIntPair coord(int index) {
			int[] lineLengths = lineLengths();
			int index1;
			int y = 0;
			while (y < lineLengths.length && 0 <= (index1 = index - (lineLengths[y] + 1))) {
				index = index1;
				y++;
			}
			return c(index, y);
		}

		private int[] lineLengths() {
			return lineLengths;
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

}
