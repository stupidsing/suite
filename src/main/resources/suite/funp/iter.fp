define.global io := consult "io.fp" ~

consult "linux.fp" ~

define.function !list.build () := do!
	let list := !new^ { elems: (array 32 * number), size: number, } ~
	{
		!append elem := do!
			let size_ := list*/size ~
			if (size_ < 32) then (
				!assign list*/elems [size_] := elem ~
				!assign list*/size := size_ + 1 ~
				()
			)
			else error
		~
		get () := list ~
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

define.function list.filter0 f := list0 => do!
	type list0 = { elems: address.of (array 32 * number), size: number, } ~
	let { elems: elems0, size: size0, } := list0 ~
	define elems1 := !new^ (array 32 * number) ~
	fold (
		(i, j) := (0, 0) #
		i != size0 #
		let i1 := i + 1 ~
		let e := elems0* [i] ~
		if (f e) then (
			!assign elems1* [j] := e ~
			i1, j + 1
		) else (
			i1, j
		) #
		{ elems: elems1, size: j, }
	)
~

define.function !list.map f := list0 => do!
	type list0 = { elems: address.of (array 32 * number), size: number, } ~
	let { elems: elems0, size: size, } := list0 ~
	define elems1 := !new^ (array 32 * number) ~
	fold (
		i := 0 #
		i != size #
		!assign elems1* [i] := f elems0* [i] ~
		i + 1 #
		{ elems: elems1, size, }
	)
~

define.function !iter () := 0
~

{
	!iter,
}
