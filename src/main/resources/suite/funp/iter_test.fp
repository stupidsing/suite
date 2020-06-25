let.global !list.build () := do!
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

let.global !list.iter list := do!
	type list = { elems: address.of (array 32 * number) ~ size: number ~ } ~
	let { elems, size, } := list ~
	let i := !new^ 0 ~
	{
		has.next () := i* < size ~
		!next () := do!
			let i_ := i* ~
			!assign i* := i_ + 1 ~
			elems* [i_]
		~
	}
~

let list0 := { elems: address.of predef (array 32 * 2) ~ size: 3 ~ } ~

do! (
	let iter := !list.iter list0 ~
	let out := !list.build () ~
	let elem0 := iter/!next () ~
	let elem1 := iter/!next () ~
	--iter/!free () ~
	out/!append 2 ~
	--let dummy := true ~
	let list1 := out/!get () ~
	2
	--let v := (list1/elems)* [0] ~
	--!list.free list1 ~
	--v
)
