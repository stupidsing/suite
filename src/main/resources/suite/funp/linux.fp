consult "asm.i686.fp" ~

expand buffer.size := 256 ~
expand (assert .check ~ .expr) := if .check then .expr else error ~
expand !peek .pointer := !asm.peek .pointer ~
expand (!poke (.pointer, .value) ~ .expr) := (perform !do !asm.poke .pointer .value ~ .expr) ~

define max (a, b) := if (a < b) then b else a ~
define min (a, b) := if (a < b) then a else b ~

define !mmap length := !do
	!asm.mmap length
~

define !munmap (length, pointer) := !do
	--type pointer = address.of (array buffer.size * byte) ~
	type pointer = number ~
	!asm.munmap length pointer
~

let.global alloc.pointer := 0 ~
let.global alloc.free.chain := 0 ~

define !alloc size0 := !do
	let size := max (os.ps, size0) ~
	define {
		!alloc.chain pointer := !do
			let chain := !peek pointer ~
			if (chain != 0) then (
				let pointer1 := !asm.adjust.pointer chain os.ps ~
				if (!peek chain != size) then (
					!alloc.chain coerce.pointer pointer1
				) else (
					!poke (pointer, !peek pointer1) ~
					chain
				)
			) else 0
		~
	} ~
	let p0 := !alloc.chain address.of alloc.free.chain ~
	if (p0 = 0) then (
		let ap := alloc.pointer ~
		let pointer.head := if (ap != 0) then ap else !mmap 16384 ~
		let pointer.block := !asm.adjust.pointer pointer.head os.ps ~
		!poke (pointer.head, size) ~
		assign alloc.pointer := !asm.adjust.pointer pointer.block size ~
		pointer.block
	) else p0
~

define !dealloc (size0, pointer.block) := !do
	let size := max (os.ps, size0) ~
	let pointer.head := !asm.adjust.pointer pointer.block (0 - os.ps) ~
	assert (size = !peek pointer.head) ~
	!poke (pointer.block, alloc.free.chain) ~
	assign alloc.free.chain := pointer.head ~
	{}
~

define !new.pool length := !do
	let pool := !mmap length ~
	{
		destroy {} := !munmap (length, pool) ~
		pool ~
		start := 0 ~
	}
~

define !new.mut.number init := !do
	type init = number ~
	let size := size.of init ~
	let pointer := !alloc size ~
	assign ^pointer := init ~
	{
		destroy {} := !dealloc (size, pointer) ~
		get {} := !do !peek pointer ~
		set v1 := !do (assign ^pointer := v1 ~ {}) ~
	}
~

define !read (pointer, length) := !do
	type pointer = address.of (array _ * byte) ~
	!asm.read pointer length
~

define !write (pointer, length) := !do
	type pointer = address.of (array _ * byte) ~
	!asm.write pointer length
~

define !write.all (pointer, length) :=
	type pointer = address.of (array _ * byte) ~
	!for (n = length; 0 < n;
		let p1 := !asm.adjust.pointer pointer (length - n) ~
		let n1 := !write (coerce.pointer p1, n) ~
		assert (n1 != 0) ~
		n - n1
	)
~

define !get.char {} := !do
	let.global buffer := array buffer.size * byte ~
	let.global start.end := (0, 0) ~
	let (s0, e0) := start.end ~
	let (s1, e1) := if (s0 < e0) then start.end else (0, !read (address.of buffer, buffer.size)) ~
	assert (s1 < e1) ~
	assign start.end := (s1 + 1, e1) ~
	buffer [s0]
~

define !put.char ch := !write.all (address.of predef [ch,], 1) ~

define !put.number n :=
	define {
		!put.number_ n := !do
			if (0 < n) then (
				let div := n / 10 ~
				let mod := n % 10 ~
				!put.number_ div ~
				!put.char coerce.byte (mod + number '0')
			) else {}
		~
	} ~
	case
	|| 0 < n =>
		!put.number_ n
	|| n < 0 => !do
		!put.char byte '-' ~
		!put.number_ n
	|| !put.char byte '0'
~

define !put.string s :=
	!for (i = 0; (^s) [i] != byte 0;
		!put.char (^s) [i] ~
		i + 1
	)
~

define !cat {} :=
	!for (n = 1; n != 0;
		let pointer := address.of predef (array buffer.size * byte) ~
		let nBytesRead := !read (pointer, buffer.size) ~
		!write.all (pointer, nBytesRead) ~
		nBytesRead
	)
~

{
	!alloc,
	!cat,
	!dealloc,
	!get.char,
	!mmap,
	!munmap,
	!put.char,
	!put.number,
	!put.string,
	!read,
	!write,
}
