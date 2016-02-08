#!/usr/bin/python
import os, sys

execfile(os.path.dirname(sys.argv[0]) + '/kbsc.py')

# redraws screen region regionrect, with top left base baserowcol, using file content in lines.
def redraw(lines, baserowcol, scregionrect):
	(baserow, basecol) = baserowcol
	((row0, col0), (row1, col1)) = scregionrect
	for row in xrange(row0, row1):
		filerow = row + baserow
		line = lines[filerow] if filerow < len(lines) else ''
		scmovecursor((row, col0))
		text = line[basecol + col0:basecol + col1]
		sys.stdout.write(text + '.' * (col1 - col0 - len(text)))

lines = []
kbinit()

lines = ['		  '] * 10

scsizerow, scsizecol = 50, 132

try:
	baserow, basecol = 0, 0
	filerow, filecol = 0, 0
	scclear()
	redraw(lines, (baserow, basecol), ((0, 0), (scsizerow, scsizecol)))

	while True:
		if len(lines) <= filerow: lines.append('')
		nlines = len(lines)
		line = lines[filerow]
		cursorrow, cursorcol = filerow - baserow, filecol - basecol

		scmovecursor((cursorrow, cursorcol))

		c = kbget()
		if c == esc + '[A': filerow = max(filerow - 1, 0)
		elif c == esc + '[B': filerow = min(filerow + 1, nlines)
		elif c == esc + '[D': filecol = max(filecol - 1, 0)
		elif c == esc + '[C': filecol = min(filecol + 1, len(line))
		elif c == esc + '[5~': filerow = max(filerow - scsizerow / 2, 0) # page up
		elif c == esc + '[6~': filerow = min(filerow + scsizerow / 2, nlines) # page down
		elif c == esc + 'OH': filecol = 0 # home
		elif c == esc + 'OF': filecol = len(line) # end
		elif c == esc + 'OQ': break # F2
		elif c == chr(13): # enter
			line, newline = line[:filecol], line[filecol:]
			lines[filerow] = line
			filerow, filecol = filerow + 1, 0
			lines.insert(filerow, newline)
			redraw(lines, (baserow, basecol), ((0, 0), (scsizerow, scsizecol))) # TODO change to scroll
		elif c == chr(127): # backspace
			filecol1 = max(filecol - 1, 0)
			lines[filerow] = line = line[:filecol1] + line[filecol:]
			filecol = filecol1
			redraw(lines, (baserow, basecol), ((cursorrow, 0), (cursorrow + 1, scsizecol)))
		elif c == chr(3): break # ctrl-C
		elif c == 'Z':
			for i in xrange(16):
				for ch in kbget():
					print "%d " % ord(ch)
		elif c[0] != esc:
			lines[filerow] = line = line[:filecol] + c + line[filecol:]
			filecol += 1
			redraw(lines, (baserow, basecol), ((cursorrow, 0), (cursorrow + 1, scsizecol)))
finally: kbdeinit()
