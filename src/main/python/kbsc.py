import sys, tty, termios

esc = chr(27)

kbfd = sys.stdin.fileno()
kboldsettings = []
kbbuffer = ''

sccursorlocation = 0, 0
sccursorgetcount = 0

def kbinit():
	global kboldsettings
	kboldsettings = termios.tcgetattr(kbfd)
	tty.setraw(sys.stdin.fileno()) # use raw read

def kbdeinit():
	global kboldsettings
	termios.tcsetattr(kbfd, termios.TCSADRAIN, kboldsettings)

def kbget():
	def kbprocessansi(buffer):
		if buffer[0] == esc and buffer[1] == '[' and buffer[-1] == 'R':
			list = map(lambda s: int(s) - 1, buffer[2:-1].split(';'))
			sccursorlocation = list[0], list[1]
			sccursorgetcount += 1
			buffer = ''
		return buffer

	def kbisansicodeend(c):
		return c.isalpha() or c == '~'

	global kbbuffer
	while not kbbuffer:
		kbbuffer += sys.stdin.read(1)

		if kbbuffer[0] == esc:
			while len(kbbuffer) <= 2 or not kbisansicodeend(kbbuffer[-1]): kbbuffer += sys.stdin.read(1)
			kbbuffer = kbprocessansi(kbbuffer)

	result, kbbuffer = kbbuffer[0], kbbuffer[1:]
	while result[0] == esc and (len(result) <= 2 or not kbisansicodeend(result[-1])):
		ch, kbbuffer = kbbuffer[0], kbbuffer[1:]
		result += ch
	return result

def kbunget(s):
	global kbbuffer
	kbbuffer = kbbuffer + s

def scclear():
	print esc + '[2J'

def scscroll(nlines):
	while nlines < 0:
		print esc + 'D'
		nlines += 1
	while 0 < nlines:
		print esc + 'M'
		nlines -= 1

def scmovecursor(rowcol):
	print esc + ('[%d;%dH' % (rowcol[0] + 1, rowcol[1] + 1))
	sccursorlocation = rowcol

def scquerycursor():
	print esc + '[6n'
	oldcount, got = sccursorgetcount, ''
	while sccursorgetcount == oldcount: got += kbget()
	kbunget(got)
	return sccursorlocation
