expand buffer.size := 256 ~
expand io.peek .pointer := io.asm (EBX = .pointer;) { MOV (EAX, `EBX`); } ~
expand io.poke (.pointer, .value) := io.asm (EAX = .value; EBX = .pointer;) { MOV (`EBX`, EAX); } ~

define map := length =>
	let ps := [0, length, 3, 34, -1, 0,] ~
	io.asm (EAX = 90; EBX = address ps;) { INT (-128); }
~

define unmap := (length, pointer) =>
	--type pointer = address (array byte * buffer.size) ~
	type pointer = io number ~
	io.asm (EAX = 91; EBX = pointer; ECX = length;) { INT (-128); }
~

let.global alloc.pointer := 0 ~
let.global alloc.free.chain := 0 ~

-- minimum size is 4
define alloc size :=
	--define {
	--	alloc.chain pointer :=
	--		io.let chain := io.peek pointer ~
	--		if (chain != 0) then (
	--			io.let bs := io.peek chain ~
	--			let chain4 := chain + 4 ~
	--			if (bs < size) then (
	--				alloc.chain chain4
	--			) else (
	--				io.let chain1 := io.peek chain4 ~
	--				io.let _ := io.poke (pointer, chain1) ~
	--				io chain
	--			)
	--		) else (
	--			io 0
	--		)
	--	~
	--} ~
	--io.let p0 := alloc.chain (address alloc.free.chain) ~
	--if (p0 = 0) then (
		io.let _ := if (alloc.pointer = 0) then (
			io.let p := map 32768 ~
			io.assign alloc.pointer := p ~
			io {}
		) else (
			io {}
		) ~
		let pointer.head := alloc.pointer ~
		let pointer.block := pointer.head + 4 ~
		io.assign alloc.pointer := pointer.block + size ~
		io.let _ := io.poke (pointer.head, size) ~
		io pointer.block
	--) else (
	--	io p0
	--)
~

define dealloc (size, pointer.block) :=
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
	io.let (s1, e1) := if (s0 < e0) then (
		io start-end
	) else (
		read (address buffer, buffer.size) | io.map (pointer => (0, pointer))
	) ~
	if (s1 < e1) then (
		io.assign start-end := (s1 + 1, e1) ~
		io buffer/:s0
	) else (
		error
	)
~

define cat := io.fold 1 (n => n != 0) (n =>
	let buffer := array byte * buffer.size ~
	let pointer := address buffer ~
	io.let nBytesRead := read (pointer, buffer.size) ~
	io.let nBytesWrote := write (pointer, nBytesRead) ~
	io nBytesRead
) ~

{ cat, map, read, unmap, write, }
