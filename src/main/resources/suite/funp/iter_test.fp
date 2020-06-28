define.global io := consult "io.fp" ~

consult "linux.fp" ~

let.global !list.build () := do!
	let elems := !new^ (array 32 * number) ~
	let size := !new^ 0 ~
	{
		!append := capture1 (elem => do!
			let size_ := size* ~
			if (size_ < 32) then (
				!assign elems* [size_] := elem ~
				!assign size* := size_ + 1 ~
				()
			)
			else error
		) ~
		!get := capture1 (() => do!
			let size_ := size* ~
			!delete^ size ~
			{ elems, size: size_, }
		) ~
	}
~

let.global !list.free list := do!
	type list = { elems: address.of (array 32 * number) ~ size: number ~ } ~
	!delete^ list/elems ~
	()
~

let.global !list.iter list := do!
	type list = { elems: address.of (array 32 * number) ~ size: number ~ } ~
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

let.global !list.f list := do!
	type list = { elems: address.of (array 32 * number) ~ size: number ~ } ~
	let iter := !list.iter list ~
	let out := !list.build () ~
	let elem0 := iter/!next () ~
	let elem1 := iter/!next () ~
	iter/!free () ~
	out/!append 2 ~
	let dummy := true ~
	out/!get ()
~

let list0 := { elems: address.of predef (array 32 * 2) ~ size: 3 ~ } ~

do! (
	let list1 := !list.f list0 ~
	let v := (list1/elems)* [0] ~
	!list.free list1 ~
	v
)
