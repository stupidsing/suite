expand size := 256
>>
define map := length =>
	let ps := [0, length, 3, 34, -1, 0,] >>
	io.asm (EAX = 90; EBX = address ps;) { INT (-128); }
>>
define unmap := (length, pointer) =>
	--type pointer = address (size * array byte _) >>
	type pointer = io 0 >>
	io.asm (EAX = 91; EBX = pointer; ECX = length;) { INT (-128); }
>>
let.global alloc.pointer := (32768 | map)
>>
define alloc := size =>
	io.asm (EBX = address alloc.pointer; ECX = size;) { MOV (EAX, `EBX`); ADD (`EBX`, ECX); }
>>
define dealloc := (size, pointer) =>
	io.asm (EBX = size; ECX = pointer;) { }
>>
define new.pool := length =>
	let pool := (length | map) >>
	{
		destroy: ({} => length, pool | unmap),
		pool,
		length,
		start: 0,
	}
-->>
--define create.mut := init =>
--	let size := size.of init >>
--	let p := size | alloc >>
--	*p := init >> {
--		get: ({} => *p),
--		set: (v1 => (*p := v1)),
--		destroy: ({} => size, p | dealloc),
--	}
-->>
--define get.char := {} =>
--	let.global buffer := (size * array byte _) >>
--	let.global start-end := (0, 0) >>
--	start-end := io.fold start-end ((s, e) => s = e) ((s, e) => (0, (buffer, size | read))) >>
--	let (s0, e0) := start-end >>
--	start-end := (s0 + 1, e0) >>
--	buffer:s0
>>
{
	map,
	unmap,
	read: ((pointer, length) =>
		type pointer = address (size * array byte _) >>
		io.asm (EAX = 3; EBX = 0; ECX = pointer; EDX = length;) { INT (-128); } -- length in EAX
	),
	write: ((pointer, length) =>
		type pointer = address (size * array byte _) >>
		io.asm (EAX = 4; EBX = 1; ECX = pointer; EDX = length;) { INT (-128); } -- length in EAX
	),
}
