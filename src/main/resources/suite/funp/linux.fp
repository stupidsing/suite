consult "asm.${platform}.fp" ~

expand adjust-pointer! %pointer %add :=
	type %pointer pointer:numberp sum (numberp:pointer %pointer) (numberp:number %add)
~

define-function mmap! length := do
	pointer:numberp asm-mmap! length
~

define-function munmap! (pointer, length) := do
	asm-munmap! (numberp:pointer pointer) length
~

define-function read! (pointer, length) := do
	asm-read! (numberp:pointer pointer) length
~

define-function write! (pointer, length) := do
	asm-write! (numberp:pointer pointer) length
~

define-function max (a, b) := if (a < b) then b else a ~
define-function min (a, b) := if (a < b) then a else b ~
