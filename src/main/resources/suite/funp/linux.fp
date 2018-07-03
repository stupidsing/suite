expand size := 256
>>
define map := length =>
	let ps := [0, length, 3, 34, -1, 0,] >>
	io.asm (EAX = 90; EBX = address ps;) { INT (-128); }
>>
define unmap := (pointer, length) =>
	type pointer = address (size * array coerce.byte _) >>
	io.asm (EAX = 91; EBX = pointer; ECX = length;) { INT (-128); }
>>
let.global alloc.pointer := (32768 | map)
>>
define alloc := size =>
	io.asm (EBX = address alloc.pointer; ECX = size;) { MOV (EAX, `EBX`); ADD (`EBX`, ECX); }
>>
define pool.new := length => {
	pool: (length | map), length: length, start: 0,
} >>
define pool.delete := address ({ pool: pool, length: length, start: start, }) =>
	type start = 0 >>
	pool, length | unmap
-->>
--define mut.create := init =>
--	let p := size.of init | alloc >>
--	*p := init >> {
--		get: () => *p,
--		set: v1 => (*p := v1),
--	}
-->>
--define getc := () =>
--	let.global buffer := (size * array coerce.byte _) >>
--	let.global se := (0, 0) >>
--	se := io.fold se ((s, e) => s = e) ((s, e) => (0, (buffer, size | read))) >>
--	let (s0, e0) := se >>
--	se := (s0 + 1, e0) >>
--	buffer:s0
>> {
	map: map,
	unmap: unmap,
	read: ((pointer, length) =>
		type pointer = address (size * array coerce.byte _) >>
		io.asm (EAX = 3; EBX = 0; ECX = pointer; EDX = length;) { INT (-128); } -- length in EAX
	),
	write: ((pointer, length) =>
		type pointer = address (size * array coerce.byte _) >>
		io.asm (EAX = 4; EBX = 1; ECX = pointer; EDX = length;) { INT (-128); } -- length in EAX
	),
}
