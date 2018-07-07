expand buffer.size := 256 ~
expand io.peek .pointer := io.asm (EBX = .pointer;) { MOV (EAX, `EBX`); } ~
expand io.poke (.pointer, .value) := io.asm (EAX = .value; EBX = .pointer;) { MOV (`EBX`, EAX); } ~

define map := length =>
	let ps := [0, length, 3, 34, -1, 0,] ~
	io.asm (EAX = 90; EBX = address ps;) { INT (-128); }
~

define unmap (length, pointer) :=
	--type pointer = address (array byte * buffer.size) ~
	type pointer = io number ~
	io.asm (EAX = 91; EBX = pointer; ECX = length;) { INT (-128); }
~

let.global alloc.pointer := 0 ~
let.global alloc.free.chain := 0 ~

define alloc size0 :=
	let size := if (4 < size0) then size0 else 4 ~
	--define {
	--	alloc.chain pointer :=
	--		io.let chain := io.peek pointer ~
	--		if (chain != 0) then (
	--			io.let bs := io.peek chain ~
	--			let pointer1 := chain + 4 ~
	--			case
	--			|| bs != size => alloc.chain pointer1
	--			|| io.let chain1 := io.peek pointer1 ~
	--				io.let _ := io.poke (pointer, chain1) ~
	--				io chain
	--		) else (
	--			io 0
	--		)
	--	~
	--} ~
	--io.let p0 := alloc.chain (address alloc.free.chain) ~
	--if (p0 = 0) then (
		io.let pointer.head := case
		|| alloc.pointer != 0 => io alloc.pointer
		|| map 32768
		~
		let pointer.block := pointer.head + 4 ~
		io.let _ := io.poke (pointer.head, size) ~
		io.assign alloc.pointer := pointer.block + size ~
		io pointer.block
	--) else (
	--	io p0
	--)
~

define dealloc (size0, pointer.block) :=
	let pointer.head := pointer.block - 4 ~
	io.let _ := io.poke (pointer.block, alloc.free.chain) ~
	io.assign alloc.free.chain := pointer.head ~
	io {}
~

define new.pool length :=
	let pool := map length ~
	{
		destroy {} := unmap (length, pool) ~
		pool ~
		length ~
		start := 0 ~
	}
~

define create.mut.number init :=
	type init = number ~
	let size := size.of init ~
	io.let pointer := alloc size ~
	io.assign ^pointer := init ~
	io {
		destroy {} := dealloc (size, pointer) ~
		get {} := io.peek pointer ~
		set v1 := (io.assign ^pointer := v1 ~ io {}) ~
	}
~

define read (pointer, length) :=
	type pointer = address (array byte * buffer.size) ~
	io.asm (EAX = 3; EBX = 0; ECX = pointer; EDX = length;) { INT (-128); } -- length in EAX
~

define write (pointer, length) :=
	type pointer = address (array byte * buffer.size) ~
	io.asm (EAX = 4; EBX = 1; ECX = pointer; EDX = length;) { INT (-128); } -- length in EAX
~

define get.char {} :=
	let.global buffer := (array byte * buffer.size) ~
	let.global start-end := (0, 0) ~
	io.let (s0, e0) := io start-end ~
	io.let (s1, e1) := case
	|| s0 < e0 => io start-end
	|| read (address buffer, buffer.size) | io.map (pointer => (0, pointer))
	~
	case
	|| s1 < e1 =>
		io.assign start-end := (s1 + 1, e1) ~
		io buffer [s0]
~

define cat := io.fold 1 (n => n != 0) (n =>
	let pointer := address predef (array byte * buffer.size) ~
	io.let nBytesRead := read (pointer, buffer.size) ~
	io.let nBytesWrote := write (pointer, nBytesRead) ~
	io nBytesRead
) ~

{ cat, map, read, unmap, write, }
