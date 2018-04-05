package suite.dev;

import static suite.util.Friends.max;
import static suite.util.Friends.min;

import java.util.function.Predicate;

import com.sun.jna.Native;

import suite.adt.pair.Fixie_.FixieFun3;
import suite.adt.pair.Fixie_.FixieFun6;
import suite.ansi.Keyboard;
import suite.ansi.Keyboard.VK;
import suite.ansi.LibcJna;
import suite.ansi.Termios;
import suite.immutable.IRope;
import suite.immutable.IRope.IRopeList;
import suite.os.FileUtil;
import suite.primitive.Chars_;
import suite.primitive.IntMutable;
import suite.primitive.IntPrimitives.IntSink;
import suite.primitive.Ints.IntsBuilder;
import suite.primitive.Ints_;
import suite.primitive.adt.pair.IntIntPair;
import suite.util.Fail;
import suite.util.FunUtil.Sink;

// mvn compile exec:java -Dexec.mainClass=suite.dev.DevMain -Dexec.args="${COLUMNS} ${LINES}"
public class DevMain {

	private LibcJna libc = (LibcJna) Native.loadLibrary("c", LibcJna.class);

	private int wrapSize = 132;
	private int viewSizeX;
	private int viewSizeY;

	public static void main(String[] args) {
		var screenSizeX = Integer.valueOf(args[0]); // Integer.valueOf(System.getenv("COLUMNS"));
		var screenSizeY = Integer.valueOf(args[1]); // Integer.valueOf(System.getenv("LINES"));
		new DevMain(screenSizeX, screenSizeY).run();
	}

	private DevMain(int screenSizeX, int screenSizeY) {
		viewSizeX = screenSizeX;
		viewSizeY = screenSizeY - 1;
	}

	private void run() {
		var input = FileUtil.read("src/main/java/suite/dev/DevMain.java");
		Text inputText = text(IRope.ropeList(input));

		try (Termios termios = new Termios(libc);) {
			termios.clear();
			Keyboard keyboard = new Keyboard(libc);

			Sink<State> redraw = state -> state.apply((st, undo, redo, text, oc, cc) -> cc.apply((cx, cy) -> oc.apply((ox, oy) -> {
				var lines = Ints_ //
						.range(viewSizeY) //
						.map(screenY -> text.get(ox, oy + screenY, viewSizeX).replace('\t', ' ')) //
						.toArray(String.class);

				termios.cursor(false);

				for (var screenY = 0; screenY < viewSizeY; screenY++) {
					termios.gotoxy(0, screenY);
					termios.puts(lines[screenY]);
				}

				termios.gotoxy(cx - ox, cy - oy);
				termios.cursor(true);
				return null;
			})));

			State state0 = new State(null, null, inputText, c(0, 0), c(0, 0));
			redraw.sink(state0);

			FixieFun3<VK, Character, State, State> mutate = (vk, ch, state) -> state //
					.apply((st, undo, redo, text, oc, cc) -> oc.apply((ox, oy) -> cc.apply((cx, cy) -> {
						int ci = text.index(cx, cy);

						if (vk == VK.LEFT_)
							return st.cursor(ci - 1);
						else if (vk == VK.RIGHT)
							return st.cursor(ci + 1);
						else if (vk == VK.UP___)
							return st.cursor(cx, cy - 1);
						else if (vk == VK.DOWN_)
							return st.cursor(cx, cy + 1);
						else if (vk == VK.PGUP_)
							return st.cursor(cx, cy - viewSizeY);
						else if (vk == VK.PGDN_)
							return st.cursor(cx, cy + viewSizeY);
						else if (vk == VK.HOME_)
							return st.cursor(text.startOfLine(ci));
						else if (vk == VK.END__)
							return st.cursor(text.endOfLine(ci));
						else if (vk == VK.CTRL_HOME_)
							return st.cursor(0);
						else if (vk == VK.CTRL_END__)
							return st.cursor(text.length());
						else if (vk == VK.CTRL_LEFT_)
							return st.cursor(text.scanNext(ci, -1, ch_ -> !Character.isJavaIdentifierPart(ch_)));
						else if (vk == VK.CTRL_RIGHT)
							return st.cursor(text.scanNext(ci, 1, ch_ -> !Character.isJavaIdentifierPart(ch_)));
						else if (vk == VK.CTRL_UP___)
							return st.offset(ox, oy - 1).cursor(cx, cy - 1);
						else if (vk == VK.CTRL_DOWN_)
							return st.offset(ox, oy + 1).cursor(cx, cy + 1);
						else if (vk == VK.F7___)
							return st.offset(ox, max(cy - viewSizeY + 1, 0));
						else if (vk == VK.F8___)
							return st.offset(ox, min(cy, text.nLines()));
						else if (vk == VK.ALT_J____) {
							var index = text.endOfLine(ci);
							Text text1 = text.splice(index, index + 1, empty);
							return st.text(text1).cursor(index);
						} else if (vk == VK.BKSP_) {
							var index = ci;
							return 0 < index ? st.splice(index - 1, index, empty) : st;
						} else if (vk == VK.ALT_UP___) {
							var i1 = text.startOfLine(ci);
							if (0 < i1) {
								var i0 = text.prevLine(i1);
								var i2 = text.nextLine(i1);
								return st.splice(i2, i2, text.text.subList(i0, i1)).splice(i0, i1, empty);
							} else
								return st;
						} else if (vk == VK.ALT_DOWN_) {
							var i0 = text.startOfLine(ci);
							var i1 = text.nextLine(i0);
							if (i1 < text.length()) {
								var i2 = text.nextLine(i1);
								return st.splice(i1, i2, empty).splice(i0, i0, text.text.subList(i1, i2));
							} else
								return st;
						} else if (vk == VK.DEL__)
							return st.splice(1, empty);
						else if (vk == VK.CTRL_K____)
							return st.splice(ci, text.endOfLine(ci), empty);
						else if (vk == VK.CTRL_U____)
							return st.splice(text.startOfLine(ci), ci, empty);
						else if (vk == VK.CTRL_D____)
							return st.splice(text.startOfLine(ci), text.nextLine(ci), empty);
						else if (vk == VK.CTRL_Y____)
							return redo != null ? redo : st;
						else if (vk == VK.CTRL_Z____) {
							State undo1 = undo != null ? undo : st;
							return new State(undo1.undo, st, undo1.text, oc, undo1.cursorCoord);
						} else if (vk == VK.CTRL_C____)
							return Fail.t();
						else if (ch != null)
							if (ch == 13) {
								var i0 = text.startOfLine(ci);
								var ix = i0;
								char ch_;
								while ((ch_ = text.at(ix)) == ' ' || ch_ == '\t')
									ix++;
								return st.splice(0, IRope.ropeList("\n").concat(text.text.subList(i0, ix)));
							} else
								return st.splice(0, IRope.ropeList(Character.toString(ch)));
						else
							return st;
					}))).apply((st, undo, redo, text, oc, cc) -> oc.apply((ox, oy) -> cc.apply((cx, cy) -> {
						IntIntPair cc_ = text.coord(sat(text.index(cx, cy), 0, text.length()));
						return st.cursor(cc_.t0, cc_.t1);
					}))).apply((st, undo, redo, text, oc, cc) -> oc.apply((ox, oy) -> cc.apply((cx, cy) -> {
						int x0 = Math.max(0, cx - viewSizeX + 1);
						int y0 = Math.max(0, cy - viewSizeY + 1);
						int ox_ = sat(ox, x0, cx);
						int oy_ = sat(oy, y0, cy);
						return st.offset(ox_, oy_);
					})));

			keyboard.loop(signal -> signal //
					.fold(state0, (state, pair_) -> pair_.map((vk, ch) -> mutate.apply(vk, ch, state))) //
					.wire(redraw));
		}
	}

	private class State {
		private State undo;
		private State redo;
		private Text text;
		private IntIntPair offsetCoord;
		private IntIntPair cursorCoord;

		private State(State undo, State redo, Text text, IntIntPair offsetCoord, IntIntPair cursorCoord) {
			this.undo = undo;
			this.redo = redo;
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
			for (int i = 0; i < 16 && (state1 = state.undo) != null; i++)
				state = state1;
			if (state != null)
				state.undo = null;
			return new State(this, null, text, offsetCoord, cursorCoord);
		}

		private State offset(int ox, int oy) {
			return new State(undo, redo, text, c(ox, oy), cursorCoord);
		}

		private State cursor(int index) {
			IntIntPair coord = text.coord(index);
			return cursor(coord.t0, coord.t1);
		}

		private State cursor(int cx, int cy) {
			return new State(undo, redo, text, offsetCoord, c(cx, cy));
		}

		private <R> R apply(FixieFun6<State, State, State, Text, IntIntPair, IntIntPair, R> fun) {
			return fun.apply(this, undo, redo, text, offsetCoord, cursorCoord);
		}
	}

	private Text text(IRopeList<Character> text) {
		IntsBuilder starts = new IntsBuilder();
		IntsBuilder ends = new IntsBuilder();
		IntMutable p0 = IntMutable.of(-1);
		var size = text.size();
		IntSink lf = px -> {
			starts.append(p0.get() + 1);
			ends.append(px);
			p0.update(px);
		};
		for (var p = 0; p < size; p++) {
			var ch = text.get(p);
			if (ch == '\n' || wrapSize < p - p0.get())
				lf.sink(p);
		}
		if (1 < size - p0.get())
			lf.sink(size);
		return new Text(text, starts.toInts().toArray(), ends.toInts().toArray());
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
			var i0 = start(py) + px;
			var ix = end(py);
			return new String(Chars_.toArray(length, i_ -> {
				var i = i_ + i0;
				return i < ix ? text.get(i) : ' ';
			}));
		}

		private Text splice(int i0, int i1, IRopeList<Character> s) {
			int i1_ = min(i1, length());
			return text(text.left(i0).concat(s.concat(text.right(i1_))));
		}

		private int prevLine(int index) {
			return startOfLine(prevLineFeed(index));
		}

		private int nextLine(int index) {
			return nextLineFeed(index) + 1;
		}

		private int startOfLine(int index) {
			return prevLineFeed(index) + 1;
		}

		private int endOfLine(int index) {
			return nextLineFeed(index);
		}

		private int prevLineFeed(int index) {
			return scanNext(index, -1, ch -> ch == '\n');
		}

		private int nextLineFeed(int index) {
			return scan(index, 1, ch -> ch == '\n');
		}

		private int scanNext(int index, int dir, Predicate<Character> pred) {
			return scan(index + dir, dir, pred);
		}

		private int scan(int index, int dir, Predicate<Character> pred) {
			while (0 <= index && index < text.size() && !pred.test(text.get(index)))
				index += dir;
			return index;
		}

		private int index(int px, int py) {
			return min(start(py) + px, end(py));
		}

		private IntIntPair coord(int index) {
			var nLines = nLines();
			int y = 0, y1;
			while ((y1 = y + 1) <= nLines && start(y1) <= index)
				y = y1;
			return c(index - start(y), y);
		}

		private int start(int y) {
			return y < 0 ? 0 : nLines() <= y ? length() : starts[y];
		}

		private int end(int y) {
			return y < 0 ? 0 : nLines() <= y ? length() : ends[y];
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

	private IRopeList<Character> empty = IRope.ropeList("");

	private static IntIntPair c(int x, int y) {
		return IntIntPair.of(x, y);
	}

	private static int sat(int x, int min, int max) {
		return min(max(x, min), max);
	}

}
