consult "asm.${platform}.fp" ~

expand !adjust.pointer .pointer .add :=
	type .pointer pointer:numberp sum (numberp:pointer .pointer) (numberp:number .add)
~

define.global !mmap length := do!
	pointer:numberp !asm.mmap length
~

define.global !munmap (pointer, length) := do!
	!asm.munmap (numberp:pointer pointer) length
~

define.global !read (pointer, length) := do!
	!asm.read (numberp:pointer pointer) length
~

define.global !write (pointer, length) := do!
	!asm.write (numberp:pointer pointer) length
~

define.global max (a, b) := if (a < b) then b else a ~
define.global min (a, b) := if (a < b) then a else b ~
