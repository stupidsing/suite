consult "linux.fp" ~

define.virtual ps.block := {
	size: numberp,
	next: address.of ps.block,
} ~

let.global alloc.pointer := type (address.of ps.block) null ~
let.global alloc.free.chain := type (address.of ps.block) null ~

-- can replace by default !new
define.global !alloc size0 := do!
	let size1 := max (os.ps, size0) ~
	let sizep := numberp:number size1 ~
	let p0 := (
		expand ps := pointer* ~
		fold (
			(pointer, pr) := address.of alloc.free.chain, null #
			pr = null && ps != null #
			if (ps*/size != sizep) then (
				address.of ps*/next, null
			) else (
				!assign ps := ps*/next ~
				null, ps
			) #
			pr
		)
	) ~
	if (p0 = null) then (
		let ap := alloc.pointer ~
		let ps := if (ap != null) then ap else !mmap 16384 ~
		let pointer.block := !adjust.pointer ps os.ps ~
		!assign alloc.pointer := !adjust.pointer pointer.block size1 ~
		!assign ps*/size := sizep ~
		pointer.block
	) else p0
~

-- can replace by default !delete
define.global !dealloc (size0, pointer.block) := do!
	let ps := !adjust.pointer pointer.block (0 - os.ps) ~
	!assign ps* := type ps.block {
		size: numberp:number max (os.ps, size0),
		next: alloc.free.chain,
	} ~
	!assign alloc.free.chain := ps ~
	()
~

define.global !alloc.mut.number init := do!
	type init = number ~
	let size := size.of init ~
	let address := !alloc size ~
	let pointer := pointer:pointer address ~
	!assign pointer* := init ~
	{
		destroy () := !dealloc (size, address) ~
		get () := do! pointer* ~
		set v1 := do! (!assign pointer* := v1 ~ ()) ~
	}
~

{
	!alloc,
	!alloc.mut.number,
	!dealloc,
}
