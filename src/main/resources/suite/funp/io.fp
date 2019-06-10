consult "linux.fp" ~

-- global functions should not reference non-global functions

expand buffer.size := 256 ~

define !new.mut.number init := do!
	type init = number ~
	let pointer := !new^ init ~
	!assign pointer* := init ~
	{
		destroy () := do! (!delete^ pointer ~ ()) ~
		get () := do! pointer* ~
		set v1 := do! (!assign pointer* := v1 ~ ()) ~
	}
~

define.global !write.all (pointer, length) :=
	for! (
		n := 0 #
		n < length #
		let p1 := !adjust.pointer pointer n ~
		let nBytesWritten := !write (p1, length - n) ~
		assert (nBytesWritten != 0) ~
		n + nBytesWritten #
		()
	)
~

define.global !get.char () := do!
	let.global buffer := array buffer.size * byte ~
	let.global start.end := (0, 0) ~
	let (s0, e0) := start.end ~
	let (s1, e1) := if (s0 < e0) then start.end else (0, !read (address.of buffer, buffer.size)) ~
	assert (s1 < e1) ~
	!assign start.end := (s1 + 1, e1) ~
	buffer [s0]
~

define !get.line (pointer, length) :=
	for! (
		(n, ch) := (0, !get.char ()) #
		n < length && number:byte ch != 10 #
		!assign (!adjust.pointer pointer n)* := ch ~
		(n + 1, !get.char ()) #
		()
	)
~

define !get.number () := do!
	let !gc () := do! number:byte !get.char () ~
	let ch0 := !gc () ~
	let positive := ch0 != number '-' ~
	fold (
		(n, ch) := (0, if positive then ch0 else !gc ()) #
		number '0' <= ch && ch <= number '9' #
		(n * 10 + ch - number '0', !gc ()) #
		if positive then n else (0 - n)
	)
~

define !get.string (pointer, length) :=
	for! (
		(n, b) := (0, true) #
		n < length && b #
		let p1 := !adjust.pointer pointer n ~
		let nBytesRead := !read (p1, 1) ~
		(n + nBytesRead, p1* != byte 10) #
		()
	)
~

define.global !put.char ch :=
	type ch = byte ~
	!write.all (address.of predef [ch,], 1)
~

define !put.line () :=
	!put.char byte 10
~

define !put.number n :=
	let {
		!put.number_ i := do!
			if (0 < i) then (
				let (div, mod) := (i / 10, i % 10) ~
				!put.number_ div ~
				!put.char byte:number (mod + number '0')
			) else ()
		~
	} ~
	case
	|| 0 < n =>
		!put.number_ n
	|| n < 0 => do!
		!put.char byte '-' ~
		!put.number_ (0 - n)
	|| !put.char byte '0'
~

define !put.string s :=
	for! (
		i := 0 #
		s* [i] != byte 0 #
		!put.char s* [i] ~
		i + 1 #
		()
	)
~

define !cat () :=
	for! (
		n := 1 #
		n != 0 #
		let pointer := address.of predef (array buffer.size * byte) ~
		let nBytesRead := !read (pointer, buffer.size) ~
		!write.all (pointer, nBytesRead) ~
		nBytesRead #
		0
	)
~

{
	!cat,
	!get.char,
	!get.line,
	!get.number,
	!get.string,
	!mmap,
	!munmap,
	!put.char,
	!put.line,
	!put.number,
	!put.string,
	!read,
	!write,
}
