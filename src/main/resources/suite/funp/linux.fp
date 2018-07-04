expand size := 256 ~

define map := length =>
	let ps := [0, length, 3, 34, -1, 0,] ~
	io.asm (EAX = 90; EBX = address ps;) { INT (-128); }
~

define unmap := (length, pointer) =>
	--type pointer = address (array size * byte) ~
	type pointer = io number ~
	io.asm (EAX = 91; EBX = pointer; ECX = length;) { INT (-128); }
~

let.global alloc.pointer := 32768 | map ~

define alloc := size =>
	io.asm (EBX = address alloc.pointer; ECX = size;) { MOV (EAX, `EBX`); ADD (`EBX`, ECX); }
~

define dealloc := (size, pointer) =>
	io.asm (EBX = size; ECX = pointer;) { }
~

define new.pool := length =>
	let pool := length | map ~
	{
		destroy: ({} => length, pool | unmap),
		pool,
		length,
		start: 0,
	}
~

--define create.mut := init =>
--	let size := size.of init ~
--	let p := size | alloc ~
--	let destroy := {} => size, p | dealloc ~
--	let get := {} => ^p ~
--	let set := v1 => (io.assign ^p := v1) ~
--	io.assign ^p := init ~
--	{
--		destroy,
--		get,
--		set,
--	}
--~

--define get.char := {} =>
--	let.global buffer := (array size * byte) ~
--	let.global start-end := (0, 0) ~
--	io.assign start-end := io.fold start-end ((s, e) => s = e) ((s, e) => (0, (buffer, size | read))) ~
--	let (s0, e0) := start-end ~
--	io.assign start-end := (s0 + 1, e0) ~
--	buffer/:s0
--~

define read := (pointer, length) =>
	type pointer = address (array size * byte) ~
	io.asm (EAX = 3; EBX = 0; ECX = pointer; EDX = length;) { INT (-128); } -- length in EAX
~

define write := (pointer, length) =>
	type pointer = address (array size * byte) ~
	io.asm (EAX = 4; EBX = 1; ECX = pointer; EDX = length;) { INT (-128); } -- length in EAX
~

define cat := io.fold 1 (n => n != 0) (n => 
	let buffer := array size * byte ~ 
	let pointer := address buffer ~ 
	pointer, size | read | io.cat (nBytesRead => 
		pointer, nBytesRead | write | io.cat (nBytesWrote => 
			io nBytesRead 
		) 
	) 
) ~

{ cat, map, read, unmap, write, }
