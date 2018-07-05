expand buffer.size := 256 ~

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

define alloc := size =>
	io.let _ := if (alloc.pointer = 0) then (
		io.let p := map 32768 ~
		io.assign alloc.pointer := p ~
		io ({})
	) else (
		io ({})
	) ~
	let pointer.head := alloc.pointer ~
	let pointer.block := pointer.head + 4 ~
	io.assign alloc.pointer := pointer.block + size ~
	io.let _ := io.asm (EAX = pointer.head; EBX = size;) { MOV (`EAX`, EBX); } ~
	io pointer.block
~

define dealloc := (size, pointer.block) =>
	let pointer.head := pointer.block - 4 ~
	io.let _ := io.asm (EAX = pointer.head; EBX = alloc.free.chain;) { MOV (`EAX`, EBX); } ~
	io.assign alloc.free.chain := pointer.head ~
	io ({})
~

define new.pool := length =>
	let pool := map length ~
	{
		destroy := {} => unmap (length, pool) ~
		pool ~
		length ~
		start := 0 ~
	}
~

define create.mut.number := init =>
	type init = number ~
	let size := size.of init ~
	io.let pointer := alloc size ~
	io.assign ^pointer := init ~
	io ({
		destroy := {} => dealloc (size, pointer) ~
		get := {} => io.asm (EBX = pointer;) { MOV (EAX, `EBX`); } ~
		set := v1 => (io.assign ^pointer := v1 ~ io ({})) ~
	})
~

define read := (pointer, length) =>
	type pointer = address (array byte * buffer.size) ~
	io.asm (EAX = 3; EBX = 0; ECX = pointer; EDX = length;) { INT (-128); } -- length in EAX
~

define write := (pointer, length) =>
	type pointer = address (array byte * buffer.size) ~
	io.asm (EAX = 4; EBX = 1; ECX = pointer; EDX = length;) { INT (-128); } -- length in EAX
~

define get.char := {} =>
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
