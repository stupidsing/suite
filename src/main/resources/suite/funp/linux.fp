define map := `size` =>
	let ps := [0, size, 3, 34, -1, 0,] >>
	let p := asm (EAX = 90; EBX = address ps;) {
		INT (-128);
	} >>
	p
>>
define unmap := `pointer, size` =>
	type pointer = address (size * array coerce-byte _) >>
	asm (EAX = 91; EBX = pointer; ECX = size;) {
		INT (-128);
	}
>>
define pool-new := `size` =>
	{ pool: (size | map), size: size, start: 0, }
>>
define pool-delete := `{ pool: pool, size: size, start: start, }` =>
	pool, size | unmap
>>
expand size := 256 >> {
	map: map,
	unmap: unmap,
	read: (`pointer, length` =>
		type pointer = address (size * array coerce-byte _) >>
		asm (EAX = 3; EBX = 0; ECX = pointer; EDX = length;) {
			INT (-128); -- length in EAX
		}
	),
	write: (`pointer, length` =>
		type pointer = address (size * array coerce-byte _) >>
		asm (EAX = 4; EBX = 1; ECX = pointer; EDX = length;) {
			INT (-128); -- length in EAX
		}
	),
}
