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

let.global alloc.pointer := map 32768 ~

define alloc := size =>
	io.asm (EBX = address alloc.pointer; ECX = size;) { MOV (EAX, `EBX`); ADD (`EBX`, ECX); }
~

define dealloc := (size, pointer) =>
	io.asm (EBX = size; ECX = pointer;) {}
~

define new.pool := length =>
	let pool := map length ~
	{
		destroy: ({} => unmap (length, pool)),
		pool,
		length,
		start: 0,
	}
~

define create.mut.number := init =>
	type init = 0 ~
	let size := size.of init ~
	let pointer := alloc size ~
	let destroy := {} => dealloc (size, pointer) ~
	let get := {} => io.asm (EBX = pointer;) { MOV (EAX, `EBX`); } ~
--	let set := v1 => (io.assign ^pointer := v1) ~
--	io.assign ^pointer := init ~
	{
		destroy,
		get,
--		set,
	}
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
	io start-end =>
	io.concat.map (s0, e0) =>
	if (s0 < e0) then (
		io start-end
	) else (
		read (address buffer, buffer.size) | io.map (pointer => (0, pointer))
	) =>
	io.concat.map (s1, e1) =>
	if (s1 < e1) then (
		io.assign start-end := (s1 + 1, e1) ~
		buffer/:s0
	) else (
		error
	)
~

define cat := io.fold 1 (n => n != 0) (n =>
	let buffer := array byte * buffer.size ~
	let pointer := address buffer ~
	read (pointer, buffer.size) =>
	io.concat.map nBytesRead => write (pointer, nBytesRead) =>
	io.concat.map nBytesWrote => io nBytesRead
) ~

{ cat, map, read, unmap, write, }
