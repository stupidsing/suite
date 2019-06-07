consult "asm.${platform}.fp" ~

expand null := pointer:number 0 ~
expand (assert .check ~ .expr) := if .check then .expr else error ~

expand !adjust.pointer .pointer .add :=
	type .pointer = address.of.any ~
	pointer:numberp !asm.adjust.pointer .pointer .add
~

define !mmap length := do!
	pointer:numberp !asm.mmap length
~

define !munmap (length, pointer) := do!
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
