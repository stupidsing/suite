define.global io := consult "io.fp" ~

consult "linux.fp" ~

define.function !list.build () := do!
	let elems := !new^ (array 32 * number) ~
	let size := !new^ 0 ~
	{
		!append elem := do!
			let size_ := size* ~
			if (size_ < 32) then (
				!assign elems* [size_] := elem ~
				!assign size* := size_ + 1 ~
				()
			)
			else error
		~
		!get () := do!
			let size_ := size* ~
			!delete^ size ~
			{ elems, size: size_, }
		~
	}
~

define.function !list.free list :=  do!
	type list = { elems: address.of (array 32 * number), size: number, } ~
	!delete^ list/elems ~
	()
~

define.function !list.iter list := do!
	type list = { elems: address.of (array 32 * number), size: number, } ~
	let { elems, size, } := list ~
	let i := !new^ 0 ~
	{
		!free () := do! (!delete^ i ~ ()) ~
		has.next () := i* < size ~
		!next () := do!
			let i_ := i* ~
			!assign i* := i_ + 1 ~
			elems* [i_]
		~
	}
~

define.function list.filter f := list => do!
	let in := !list.iter list ~
	let out := !list.build () ~
	fold (
		b := true #
		b #
		if (in/has.next ()) then (
			let elem := in/!next () ~
			if (f elem) then (
				out/!append elem ~ true
			) else true
		) else false
		#
		in/!free () ~
		out/!get ()
	)
~

define.function list.map f := list => do!
	let in := !list.iter list ~
	let out := !list.build () ~
	fold (
		b := true #
		b #
		if (in/has.next ()) then (out/!append (f (in/!next ())) ~ true) else false #
		in/!free () ~
		out/!get ()
	)
~

{
	!list.iter,
}
