package suite.ansi;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import com.sun.jna.Native;

import primal.Verbs.ReadString;
import primal.fp.Funs.Sink;

// mvn compile exec:java -Dexec.mainClass=suite.ansi.TermEditorMain
public class TermEditorMain {

	private LibcJna libc = (LibcJna) Native.load("c", LibcJna.class);

	public static void main(String[] args) {
		new TermEditorMain().run();
	}

	private record Action(int x0, int x1, int y, String oldString, String newString) {
	}

	private record Handle( //
			Map<Integer, Handle> map, //
			Sink<Integer> sink) {
		private static Handle of(Map<Integer, Handle> map) {
			return of(map, ch -> {
			});
		}

		private static Handle of(Sink<Integer> sink) {
			return of(Collections.emptyMap(), sink);
		}

		private static Handle of(Map<Integer, Handle> map, Sink<Integer> sink) {
			return new Handle(map, sink);
		}
	}

	private void run() {
		try (var termios = new Termios(libc)) {
			var text = ReadString.from("src/main/java/suite/ansi/TermEditorMain.java");
			var lines = new HashMap<Integer, String>();
			var nLines = 0;

			for (var line : text.split("\n"))
				lines.put(nLines++, line);

			var nLines_ = nLines;

			var size = termios.getSize();
			var nCols = size.t1;
			var nRows = size.t0;

			termios.clear();

			var o = new Object() {
				private int basex = 0, basey = 0;
				private int cursorx = 0, cursory = 0;
				private boolean cont = true;

				private void moveCursor(int dx, int dy) {
					int x1 = Math.max(0, cursorx + dx);
					int y1 = Math.max(0, Math.min(nLines_, cursory + dy));
					if (x1 < basex) {
						basex = x1;
						redraw();
					}
					if (basex < x1 - (nCols - 1)) {
						basex = x1 - (nCols - 1);
						redraw();
					}
					if (y1 < basey) {
						basey = y1;
						redraw();
					}
					if (basey < y1 - (nRows - 1)) {
						basey = y1 - (nRows - 1);
						redraw();
					}
					gotoCursor(x1, y1);
				}

				private void redraw() {
					for (var r = 0; r < nRows; r++)
						redrawRow(r);

					setCursor();
				}

				private void redrawRow(int r) {
					var y_ = r + basey;
					var line = y_ < lines.size() ? lines.get(y_) : "";

					termios.gotoxy(0, r);

					for (var c = 0; c < nCols; c++) {
						var x_ = c + basex;
						var ch = 0 <= x_ && x_ < line.length() ? line.charAt(x_) : ' ';
						termios.putc(ch != 9 ? ch : ' ');
					}
				}

				private void gotoCursor(int cx, int cy) {
					cursorx = cx;
					cursory = cy;
					if (cx < basex) {
						basex = cx;
						redraw();
					}
					if (cy < basey) {
						basey = cy;
						redraw();
					}
					setCursor();
				}

				private void setCursor() {
					var x = cursorx - basex;
					var y = cursory - basey;
					if (0 <= x && x < nCols && 0 <= y && y < nRows)
						termios.gotoxy(x, y);
				}
			};

			var a = new Object() {
				private Deque<Action> actions = new ArrayDeque<>();

				private void splice(int x0, int x1, int y, String newString) {
					var line = lines.get(y);
					String oldString = line.substring(x0, x1);
					lines.put(y, line.substring(0, x0) + newString + line.substring(x1));
					actions.push(new Action(x0, x1, y, oldString, newString));
				}

				private void undo() {
					if (0 < actions.size()) {
						var action = actions.pop();
						var line = lines.get(action.y);
						var l = line.substring(0, action.x0);
						var r = line.substring(action.x0 + action.newString.length());
						lines.put(action.y, l + action.oldString + r);
						o.redrawRow(action.y - o.basey);
					}
				}
			};

			var c = new Object() {
				private void delete() {
					a.splice(o.cursorx, o.cursorx + 1, o.cursory, "");
					o.redrawRow(o.cursory - o.basey);
					o.setCursor();
				}

				private void end() {
					var line = lines.get(o.cursory);
					o.gotoCursor(line != null ? line.length() : 0, o.cursory);
				}

				private void moveCursor(int dx, int dy) {
					o.moveCursor(dx, dy);
				}

				private void insert(char ch) {
					a.splice(o.cursorx, o.cursorx, o.cursory, String.valueOf(ch));
					o.redrawRow(o.cursory - o.basey);
					o.moveCursor(1, 0);
				}

				private void pageDown() {
					if (o.basey == o.cursory) {
						o.cursory = o.basey + (nRows - 1);
						o.setCursor();
					} else {
						if (o.basey + (nRows - 1) == o.cursory) {
							o.basey = Math.min(nLines_, o.basey + nRows);
							o.cursory = o.basey + (nRows - 1);
						} else
							o.basey = o.cursory;
						o.redraw();
					}
				}

				private void pageUp() {
					if (o.basey + (nRows - 1) == o.cursory) {
						o.cursory = o.basey;
						o.setCursor();
					} else {
						if (o.basey == o.cursory) {
							o.basey = Math.max(0, o.basey - nRows);
							o.cursory = o.basey;
						} else
							o.basey = Math.max(0, o.cursory - (nRows - 1));
						o.redraw();
					}

				}
			};
			o.redraw();

			Handle handle = //
					Handle.of(Map.ofEntries( //
							Map.entry(24, Handle.of(ch -> o.cont = false)), //
							Map.entry(26, Handle.of(ch -> a.undo())), //
							Map.entry(27, Handle.of(Map.ofEntries( //
									Map.entry(91, Handle.of(Map.ofEntries( //
											Map.entry(51, Handle.of(Map.ofEntries( //
													Map.entry(126, Handle.of(ch -> c.delete())) //
											))), //
											Map.entry(53, Handle.of(Map.ofEntries( //
													Map.entry(126, Handle.of(ch -> c.pageUp())) //
											))), //
											Map.entry(54, Handle.of(Map.ofEntries( //
													Map.entry(126, Handle.of(ch -> c.pageDown())) //
											))), //
											Map.entry(65, Handle.of(ch -> c.moveCursor(0, -1))), // up
											Map.entry(66, Handle.of(ch -> c.moveCursor(0, +1))), // down
											Map.entry(67, Handle.of(ch -> c.moveCursor(+1, 0))), // right
											Map.entry(68, Handle.of(ch -> c.moveCursor(-1, 0))), // left
											Map.entry(70, Handle.of(ch -> c.end())), //
											Map.entry(72, Handle.of(ch -> o.gotoCursor(0, o.cursory))) // home
									))) //
							))), //
							Map.entry(127, Handle.of(ch -> { // backspace
								if (0 < o.cursorx) {
									a.splice(o.cursorx - 1, o.cursorx, o.cursory, "");
									o.redrawRow(o.cursory - o.basey);
									o.moveCursor(-1, 0);
								}
							})) //
					), ch -> {
						if (32 <= ch && ch < 127)
							c.insert((char) (int) ch);
					});

			Handle handle_ = handle;

			while (o.cont) {
				var ch = libc.getchar();

				handle_.sink.f(ch);

				handle_ = handle_.map.get(ch);

				if (handle_ == null)
					handle_ = handle;
				else if (handle_.map.isEmpty()) {
					handle_.sink.f(ch);
					handle_ = handle;
				}
			}
		}
	}

}
