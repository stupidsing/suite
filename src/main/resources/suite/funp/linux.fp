expand buffer.size := 256 ~

define map := length =>
	let ps := [0, length, 3, 34, -1, 0,] ~
	io.asm (EAX = 90; EBX = address ps;) { INT (-128); }
~

define unmap := (length, pointer) =>
	--type pointer = address (array buffer.size * byte) ~
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
	type pointer = address (array buffer.size * byte) ~
	io.asm (EAX = 3; EBX = 0; ECX = pointer; EDX = length;) { INT (-128); } -- length in EAX
~

define write := (pointer, length) =>
	type pointer = address (array buffer.size * byte) ~
	io.asm (EAX = 4; EBX = 1; ECX = pointer; EDX = length;) { INT (-128); } -- length in EAX
~

define get.char := {} =>
	let.global buffer := (array buffer.size * byte) ~
	let.global start-end := (0, 0) ~
--	io.update start-end := io.fold start-end ((s, e) => s = e) ((s, e) => io (0, read (buffer, buffer.size))) ~
	let (s0, e0) := start-end ~
	io.update start-end := (s0 + 1, e0) ~
	buffer/:s0
~

define cat := io.fold 1 (n => n != 0) (n =>
	let buffer := array buffer.size * byte ~
	let pointer := address buffer ~
	read (pointer, buffer.size) =>
	io nBytesRead => write (pointer, nBytesRead) =>
	io nBytesWrote => io nBytesRead
) ~

{ cat, map, read, unmap, write, }
