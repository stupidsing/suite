expand buffer.size := 256 ~
expand (assert .check ~ .expr) := if .check then .expr else error ~
expand peek .pointer := eval.io io.asm (EBX = .pointer;) { MOV (EAX, `EBX`); } ~
expand (poke (.pointer, .value) ~ .expr) := (perform.io io.asm (EAX = .value; EBX = .pointer;) { MOV (`EBX`, EAX); } ~ .expr) ~

define max (a, b) := if (a < b) then b else a ~
define min (a, b) := if (a < b) then a else b ~

define io.mmap length :=
	let ps := [0, length, 3, 34, -1, 0,] ~
	io.asm (EAX = 90; EBX = address ps;) { INT (-128); }
~

define io.munmap (length, pointer) :=
	--type pointer = address (array byte * buffer.size) ~
	type pointer = number ~
	io.asm (EAX = 91; EBX = pointer; ECX = length;) { INT (-128); }
~

let.global alloc.pointer := 0 ~
let.global alloc.free.chain := 0 ~

define io.alloc size0 := do
	let size := max (4, size0) ~
	define {
		io.alloc.chain pointer := do
			let chain := peek pointer ~
			if (chain != 0) then (
				let pointer1 := chain + 4 ~
				if (peek chain != size) then (
					eval.io io.alloc.chain coerce.pointer pointer1
				) else (
					poke (pointer, peek pointer1) ~
					chain
				)
			) else 0
		~
	} ~
	let p0 := eval.io io.alloc.chain address alloc.free.chain ~
	if (p0 = 0) then (
		let ap := alloc.pointer ~
		let pointer.head := if (ap != 0) then ap else ap ~
		let pointer.block := pointer.head + 4 ~
		poke (pointer.head, size) ~
		assign alloc.pointer := pointer.block + size ~
		pointer.block
	) else p0
~

define io.dealloc (size0, pointer.block) := do
	let size := max (4, size0) ~
	let pointer.head := pointer.block - 4 ~
	assert (size = peek pointer.head) ~
	poke (pointer.block, alloc.free.chain) ~
	assign alloc.free.chain := pointer.head ~
	{}
~

define new.pool length := do
	let pool := eval.io io.mmap length ~
	{
		destroy {} := io.munmap (length, pool) ~
		pool ~
		length ~
		start := 0 ~
	}
~

define new.mut.number init := do
	type init = number ~
	let size := size.of init ~
	let pointer := eval.io io.alloc size ~
	assign ^pointer := init ~
	{
		destroy {} := io.dealloc (size, pointer) ~
		get {} := peek pointer ~
		set v1 := do (assign ^pointer := v1 ~ {}) ~
	}
~

define io.read (pointer, length) :=
	type pointer = address (array byte * _) ~
	io.asm (EAX = 3; EBX = 0; ECX = pointer; EDX = length;) { INT (-128); } -- length in EAX
~

define io.write (pointer, length) :=
	type pointer = address (array byte * _) ~
	io.asm (EAX = 4; EBX = 1; ECX = pointer; EDX = length;) { INT (-128); } -- length in EAX
~

define io.write.all (pointer, length) :=
	type pointer = address (array byte * _) ~
	io.for (n = length; 0 < n; do
		let p1 := eval.io io.asm (EAX = pointer; EBX = length; ECX = n;) { ADD (EAX, EBX); SUB (EAX, ECX); } ~
		let n1 := eval.io io.write (coerce.pointer p1, n) ~
		assert (n1 != 0) ~
		n - n1
	)
~

define io.get.char {} := do
	let.global buffer := array byte * buffer.size ~
	let.global start.end := (0, 0) ~
	let (s0, e0) := start.end ~
	let (s1, e1) := if (s0 < e0) then (start.end) else (0, eval.io io.read (address buffer, buffer.size)) ~
	assert (s1 < e1) ~
	assign start.end := (s1 + 1, e1) ~
	buffer [s0]
~

define io.put.char ch := io.write.all (address predef [ch,], 1) ~

define io.put.number n :=
	define {
		io.put.number_ n := do
			if (0 < n) then (
				let div := n / 10 ~
				let mod := n % 10 ~
				perform.io io.put.number_ div ~
				eval.io io.put.char coerce.byte (mod + number '0')
			) else {}
		~
	} ~
	case
	|| 0 < n =>
		io.put.number_ n
	|| n < 0 =>
		perform.io io.put.char byte '-' ~ io.put.number_ n
	|| io.put.char byte '0'
~

define io.put.string s :=
	io.for (i = 0; (^s) [i] != byte 0; do
		perform.io io.put.char (^s) [i] ~
		i + 1
	)
~

define io.cat :=
	io.for (n = 1; n != 0; do
		let pointer := address predef (array byte * buffer.size) ~
		let nBytesRead := eval.io io.read (pointer, buffer.size) ~
		perform.io io.write.all (pointer, nBytesRead) ~
		nBytesRead
	)
~

{ io.cat, io.mmap, io.munmap, io.read, io.write, }
