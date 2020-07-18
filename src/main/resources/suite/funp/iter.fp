define.global !list.build () := do!
	let elems := !new-array^ (32 * number) ~
	let dummy := fold (
	    i := 0 #
	    i < 32 #
	    !assign elems* [i] := 0 ~ i + 1 #
	    ()
	) ~
	let size := !new^ 0 ~
	let !append := elem => capture do!
		let size_ := size* ~
		if (size_ < 32) then (
			!assign elems* [size_] := elem ~
			!assign size* := size_ + 1 ~
			()
		)
		else error
	~
	{
		!append ~
		!get := () => capture1 do!
			uncapture !append ~
			let size_ := size* ~
			!delete^ size ~
			{ elems, size: size_, }
		~
	}
~

define.global !list.free list := do!
	type list = { elems: address.of (array 32 * number) ~ size: number ~ } ~
	!delete-array^ list/elems ~
	()
~

define.global !list.iter list := do!
	type list = { elems: address.of (array 32 * number) ~ size: number ~ } ~
	let { elems, size, } := list ~
	let i := !new^ 0 ~
	let has.next := () => capture (i* < size) ~
	let !next := () => capture do!
		let i_ := i* ~
		!assign i* := i_ + 1 ~
		elems* [i_]
	~
	{
		!free := () => capture1 do!
			uncapture has.next ~
			uncapture !next ~
			!delete^ i ~
			()
		~
		has.next ~
		!next ~
	}
~

define.global list.filter f := list => capture do!
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

define.global list.map f := list => capture do!
		let in := !list.iter list ~
		let out := !list.build () ~
		fold (
			b := true #
			b #
			if (in/has.next ()) then (
				let elem := in/!next () ~
				out/!append (f elem) ~
				true
			) else false #
			in/!free () ~
			out/!get ()
		)
~

{
	!list.build,
	list.filter,
	!list.free,
	!list.iter,
	list.map,
}
