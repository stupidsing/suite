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
		!get () := list ~
	}
~

define.function !list.iter list := do!
	type list = { elems: (array 32 * number), size: number, } ~
	let { elems, size, } := list ~
	let i := !new^ 0 ~
	{
		!free () := do! (!delete^ i ~ ()) ~
		has.next () := i* < size ~
		!next () := do!
			let i_ := i* ~
			!assign i* := i_ + 1 ~
			elems [i_]
		~
	}
~

define.function list.filter f := list0 =>
	type list0 = { elems: (array 32 * number), size: number, } ~
	let { elems: elems0, size: size0, } := list0 ~
	define list1 := array 32 * number ~
	fold (
		(i, j) := (0, 0) #
		i != size0 #
		let i1 := i + 1 ~
		let e := elems0 [i] ~
		if (f e) then (
			!assign list1 [j] := e ~
			i1, j + 1
		) else (
			i1, j
		) #
		{ elems: list1, size: j, }
	)
~

define.function list.map f := list0 =>
	type list0 = { elems: (array 32 * number), size: number, } ~
	let { elems: elems0, size: size0, } := list0 ~
	define list1 := array 32 * number ~
	fold (
		i := 0 #
		i != size0 #
		!assign list1 [i] := f elems0 [i] ~
		i + 1 #
		{ elems: list1, size: size0, }
	)
~

define.function !iter () := 0
~

{
	!iter,
}
