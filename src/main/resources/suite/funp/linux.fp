consult "asm.${platform}.fp" ~

expand !adjust.pointer .pointer .add :=
	type .pointer = address.of.any ~
	pointer:numberp !asm.adjust.pointer .pointer .add
~

define.global !mmap length := do!
	pointer:numberp !asm.mmap length
~

define.global !munmap (length, pointer) := do!
	type pointer = address.of.any ~
	!asm.munmap length pointer
~

define.global !read (pointer, length) := do!
	type pointer = address.of.any ~
	!asm.read pointer length
~

define.global !write (pointer, length) := do!
	type pointer = address.of.any ~
	!asm.write pointer length
~

define max (a, b) := if (a < b) then b else a ~
define min (a, b) := if (a < b) then a else b ~
