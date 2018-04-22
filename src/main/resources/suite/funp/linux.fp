expand size := 256 >>
define linux-mmap := `length` => (
	let ps := array (0, length, 3, 34, -1, 0,) >>
	let p := asm (EAX = 90; EBX = address ps;) {
		INT (-128);
	} >>
	p
) >>
define linux-munmap := `pointer, length` => (
	type pointer = address (size * array coerce-byte _) >>
	asm (EAX = 91; EBX = pointer; ECX = length;) {
		INT (-128);
	}
) >>
define linux-read := `pointer, length` => (
	type pointer = address (size * array coerce-byte _) >>
	asm (EAX = 3; EBX = 0; ECX = pointer; EDX = length;) {
		INT (-128); -- length in EAX
	}
) >>
define linux-write := `pointer, length` => (
	type pointer = address (size * array coerce-byte _) >>
	asm (EAX = 4; EBX = 1; ECX = pointer; EDX = length;) {
		INT (-128); -- length in EAX
	}
) >>
struct (
	map linux-mmap,
	unmap linux-munmap,
	read linux-read,
	write linux-write,
)
