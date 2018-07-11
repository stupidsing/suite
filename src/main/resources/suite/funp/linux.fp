expand buffer.size := 256 ~
expand (assert .check ~ .expr) := if .check then .expr else error ~
expand io.peek .pointer := io.asm (EBX = .pointer;) { MOV (EAX, `EBX`); } ~
expand io.poke (.pointer, .value) := io.asm (EAX = .value; EBX = .pointer;) { MOV (`EBX`, EAX); } ~

define max (a, b) := if (a < b) then b else a ~
define min (a, b) := if (a < b) then a else b ~

define map length :=
	let ps := [0, length, 3, 34, -1, 0,] ~
	io.asm (EAX = 90; EBX = address ps;) { INT (-128); }
~

define unmap (length, pointer) :=
	--type pointer = address (array byte * buffer.size) ~
	type pointer = number ~
	io.asm (EAX = 91; EBX = pointer; ECX = length;) { INT (-128); }
~

let.global alloc.pointer := 0 ~
let.global alloc.free.chain := 0 ~

define alloc size0 :=
	let size := max (4, size0) ~
	define {
		alloc.chain pointer :=
			io.let chain := io.peek pointer ~
			if (chain != 0) then (
				io.let bs := io.peek chain ~
				let pointer1 := chain + 4 ~
				if (bs != size) then (
					alloc.chain coerce.pointer pointer1
				) else (
					io.let chain1 := io.peek pointer1 ~
					io.perform io.poke (pointer, chain1) ~
					io chain
				)
			) else (io 0)
		~
	} ~
	io.let p0 := alloc.chain (address alloc.free.chain) ~
	if (p0 = 0) then (
		let ap := alloc.pointer ~
		io.let pointer.head := if (ap != 0) then (io ap) else (map 32768) ~
		let pointer.block := pointer.head + 4 ~
		io.perform io.poke (pointer.head, size) ~
		io.assign alloc.pointer := pointer.block + size ~
		pointer.block
	) else (io p0)
~

define dealloc (size0, pointer.block) :=
	let size := max (4, size0) ~
	let pointer.head := pointer.block - 4 ~
	io.let size_ := io.peek pointer.head ~
	assert (size = size_) ~
	io.perform io.poke (pointer.block, alloc.free.chain) ~
	io.assign alloc.free.chain := pointer.head ~
	{}
~

define new.pool length :=
	io.let pool := map length ~
	io {
		destroy {} := unmap (length, pool) ~
		pool ~
		length ~
		start := 0 ~
	}
~

define new.mut.number init :=
	type init = number ~
	let size := size.of init ~
	io.let pointer := alloc size ~
	io.assign ^pointer := init ~
	{
		destroy {} := dealloc (size, pointer) ~
		get {} := io.peek pointer ~
		set v1 := (io.assign ^pointer := v1 ~ {}) ~
	}
~

define read (pointer, length) :=
	type pointer = address (array byte * _) ~
	io.asm (EAX = 3; EBX = 0; ECX = pointer; EDX = length;) { INT (-128); } -- length in EAX
~

define write (pointer, length) :=
	type pointer = address (array byte * _) ~
	io.asm (EAX = 4; EBX = 1; ECX = pointer; EDX = length;) { INT (-128); } -- length in EAX
~

define write.all (pointer, length) :=
	type pointer = address (array byte * _) ~
	io.for (n = length; 0 < n;
		io.let p1 := io.asm (EAX = pointer; EBX = length; ECX = n;) { ADD (EAX, EBX); SUB (EAX, ECX); } ~
		io.let n1 := write (coerce.pointer p1, n) ~
		assert (n1 != 0) ~
		io (n - n1)
	)
~

define get.char {} :=
	let.global buffer := array byte * buffer.size ~
	let.global start.end := (0, 0) ~
	io.let (s0, e0) := io start.end ~
	io.let (s1, e1) := if (s0 < e0) then (io start.end) else (
		read (address buffer, buffer.size) | io.map (pointer => (0, pointer))
	) ~
	assert (s1 < e1) ~
	io.assign start.end := (s1 + 1, e1) ~
	buffer [s0]
~

define put.char ch := write.all (address predef [ch,], 1) ~

define put.number n :=
	define {
		put.number_ n := if (0 < n) then (
			let div := n / 10 ~
			let mod := n % 10 ~
			io.perform put.number_ div ~
			put.char coerce.byte (mod + number '0')
		) else (io {})
		~
	} ~
	case
	|| 0 < n =>
		put.number_ n
	|| n < 0 =>
		io.perform put.char byte '-' ~ put.number_ n
	|| put.char byte '0'
~

define put.string s :=
	io.for (i = 0; (^s) [i] != byte 0;
		io.perform put.char (^s) [i] ~
		io (i + 1)
	)
~

define cat :=
	io.for (n = 1; n != 0;
		let pointer := address predef (array byte * buffer.size) ~
		io.let nBytesRead := read (pointer, buffer.size) ~
		io.perform write.all (pointer, nBytesRead) ~
		io nBytesRead
	)
~

{ cat, map, read, unmap, write, }
