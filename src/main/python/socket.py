#!/usr/bin/python

# Sample TCP proxy server in python

from select import *
from signal import *
from socket import *
from threading import *
from time import *

class Worker(Thread):
	def __init__(self, sock):
		Thread.__init__(self)
		self.fro = sock

	def run(self):
		fror = tor = ""
		to = socket(AF_INET, SOCK_STREAM)
		to.connect(("www.google.com", 80))
		while True:
			selected = select([self.fro, to], [self.fro, to], [self.fro, to])
			for s in selected[0]:
				if s == self.fro: fror += s.recv(1024)
				elif s == to: tor += s.recv(1024)
			for s in selected[1]:
				if s == self.fro: tor = tor[s.send(tor):]
				elif s == to: fror = fror[s.send(fror):]
			for s in selected[2]:
				break
		self.fro.close()
		to.close()


signal(SIGPIPE, SIG_IGN)

listener = socket(AF_INET, SOCK_STREAM)
listener.setsockopt(SOL_SOCKET, SO_REUSEADDR, 1)
listener.bind(("", 1161))
listener.listen(32)

while select([listener], [], [], None) [0]:
	sock, address = listener.accept()

	print "ACCEPTED", strftime("%Y-%m-%d %H:%M:%S", localtime())
	Worker(sock).start()
