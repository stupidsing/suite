package suite.ansi;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.sun.jna.Native;

import primal.Verbs.ReadString;
import primal.fp.Funs.Sink;

// mvn compile exec:java -Dexec.mainClass=suite.ansi.TermEditorMain
public class TermEditorMain {

	private LibcJna libc = (LibcJna) Native.load("c", LibcJna.class);

	public static void main(String[] args) {
		new TermEditorMain().run(0 < args.length ? args[0] : null);
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

	private void run(String filename) {
		var filename_ = Objects.requireNonNullElse(filename, "src/main/java/suite/ansi/TermEditorMain.java");

		try (var termios = new Termios(libc)) {
			var f = new Object() {
				private Map<Integer, String> lines;
				private int nLines;

				private void load() {
					var text = filename_ != null ? ReadString.from(filename_) : "";

					lines = new HashMap<Integer, String>();
					nLines = 0;

					for (var line : text.split("\n"))
						lines.put(nLines++, line);
				}

				private void save() {
					var sb = new StringBuilder();

					for (var i = 0; i < nLines; i++) {
						sb.append(lines.get(i));
						sb.append("\n");
					}

					try (var fos = new FileOutputStream(filename_)) {
						fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
					} catch (IOException ex) {
						throw new RuntimeException(ex);
					}
				}

				private String getLine(int y) {
					return y < nLines ? lines.get(y) : "";
				}

				private void putLine(int y, String line) {
					while (nLines <= y)
						lines.put(nLines++, "");
					lines.put(y, line);
				}
			};

			var size = termios.getSize();
			var nCols = size.t1;
			var nRows = size.t0;

			var d = new Object() {
				private int basex = 0, basey = 0;
				private int cursorx = 0, cursory = 0;
				private boolean cont = true;

				private void moveCursor(int dx, int dy) {
					int x1 = Math.max(0, cursorx + dx);
					int y1 = Math.max(0, Math.min(f.nLines, cursory + dy));

					if (x1 < basex) {
						basex = x1 - 8;
						redraw();
					} else if (basex < x1 - (nCols - 1)) {
						basex = x1 - (nCols - 1) + 8;
						redraw();
					}

					if (y1 < basey) {
						basey = y1 - 8;
						redraw();
					} else if (basey < y1 - (nRows - 1)) {
						basey = y1 - (nRows - 1) + 8;
						redraw();
					}

					gotoCursor(x1, y1);
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

				private void redraw() {
					for (var r = 0; r < nRows; r++)
						redrawRow(r);

					setCursor();
				}

				private void redrawRow(int r) {
					var y_ = r + basey;
					var line = f.getLine(y_);

					termios.gotoxy(0, r);

					for (var c = 0; c < nCols; c++) {
						var x_ = c + basex;
						var ch = 0 <= x_ && x_ < line.length() ? line.charAt(x_) : ' ';
						termios.putc(ch != 9 ? ch : ' ');
					}
				}

				private void setCursor() {
					var x = cursorx - basex;
					var y = cursory - basey;
					if (0 <= x && x < nCols && 0 <= y && y < nRows)
						termios.gotoxy(x, y);
				}
			};

			var a = new Object() {
				private Deque<Action> redos = new ArrayDeque<>();
				private Deque<Action> undos = new ArrayDeque<>();

				private void redo() {
					if (0 < redos.size())
						do_(redos.pop());
				}

				private void splice(int x0, int x1, int y, String newString) {
					var line = f.getLine(y);
					var oldString = line.substring(x0, x1);
					var action = new Action(x0, x1, y, oldString, newString);
					do_(action);
					redos.clear();
					undos.push(action);
				}

				private void do_(Action action) {
					var line = f.getLine(action.y);
					var l = line.substring(0, action.x0);
					var r = line.substring(action.x1);
					f.putLine(action.y, l + action.newString + r);
					d.gotoCursor(action.x0 + action.newString.length(), action.y);
					d.redrawRow(action.y - d.basey);
				}

				private void undo() {
					if (0 < undos.size()) {
						var action = undos.pop();
						redos.push(action);

						var line = f.getLine(action.y);
						var l = line.substring(0, action.x0);
						var r = line.substring(action.x0 + action.newString.length());
						f.putLine(action.y, l + action.oldString + r);
						d.gotoCursor(action.x1, action.y);
						d.redrawRow(action.y - d.basey);
					}
				}
			};

			var c = new Object() {
				private void backspace() {
					if (0 < d.cursorx)
						a.splice(d.cursorx - 1, d.cursorx, d.cursory, "");
				}

				private void delete() {
					a.splice(d.cursorx, d.cursorx + 1, d.cursory, "");
				}

				private void end() {
					var line = f.getLine(d.cursory);
					d.gotoCursor(line != null ? line.length() : 0, d.cursory);
				}

				private void home() {
					d.gotoCursor(0, d.cursory);
				}

				private void insert(char ch) {
					a.splice(d.cursorx, d.cursorx, d.cursory, String.valueOf(ch));
				}

				private void moveCursor(int dx, int dy) {
					d.moveCursor(dx, dy);
				}

				private void pageDown() {
					if (d.basey == d.cursory) {
						d.cursory = d.basey + (nRows - 1);
						d.setCursor();
					} else {
						if (d.basey + (nRows - 1) == d.cursory) {
							d.basey = Math.min(f.nLines, d.basey + nRows);
							d.cursory = d.basey + (nRows - 1);
						} else
							d.basey = d.cursory;
						d.redraw();
					}
				}

				private void pageUp() {
					if (d.basey + (nRows - 1) == d.cursory) {
						d.cursory = d.basey;
						d.setCursor();
					} else {
						if (d.basey == d.cursory) {
							d.basey = Math.max(0, d.basey - nRows);
							d.cursory = d.basey;
						} else
							d.basey = Math.max(0, d.cursory - (nRows - 1));
						d.redraw();
					}

				}
			};

			f.load();
			termios.clear();
			d.redraw();

			Handle handle = //
					Handle.of(Map.ofEntries( //
							Map.entry(19, Handle.of(ch -> f.save())), //
							Map.entry(24, Handle.of(ch -> d.cont = false)), //
							Map.entry(25, Handle.of(ch -> a.redo())), //
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
											Map.entry(72, Handle.of(ch -> c.home())) //
									))) //
							))), //
							Map.entry(127, Handle.of(ch -> c.backspace())) //
					), ch -> {
						if (32 <= ch && ch < 127)
							c.insert((char) (int) ch);
					});

			Handle handle_ = handle;

			while (d.cont) {
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
