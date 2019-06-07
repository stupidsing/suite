consult "asm.${platform}.fp" ~

expand null := pointer:number 0 ~
expand buffer.size := 256 ~
expand (assert .check ~ .expr) := if .check then .expr else error ~

expand !adjust.pointer .pointer .add :=
	type .pointer = address.of.any ~
	pointer:numberp !asm.adjust.pointer .pointer .add
~

define max (a, b) := if (a < b) then b else a ~
define min (a, b) := if (a < b) then a else b ~

define !mmap length := do!
	pointer:numberp !asm.mmap length
~

define !munmap (length, pointer) := do!
	type pointer = address.of.any ~
	!asm.munmap length pointer
~

virtual ps.block := {
	size: numberp,
	next: address.of ps.block,
} ~

let.global alloc.pointer := type (address.of ps.block) null ~
let.global alloc.free.chain := type (address.of ps.block) null ~

-- can replace by default !new
define !alloc size0 := do!
	let size1 := max (os.ps, size0) ~
	let sizep := numberp:number size1 ~
	define {
		!alloc.chain p :=
			expand ps := pointer* ~
			for! (
				(pointer, pr) := p, null #
				pr = null && ps != null #
				if (ps*/size != sizep) then (
					address.of ps*/next, null
				) else (
					!assign ps := ps*/next ~
					null, ps
				) #
				pr
			)
		~
	} ~
	let p0 := !alloc.chain address.of alloc.free.chain ~
	if (p0 = null) then (
		let ap := alloc.pointer ~
		let ps := if (ap != null) then ap else !mmap 16384 ~
		let pointer.block := !adjust.pointer ps os.ps ~
		!assign ps*/size := sizep ~
		!assign alloc.pointer := !adjust.pointer pointer.block size1 ~
		pointer.block
	) else p0
~

-- can replace by default !delete
define !dealloc (size0, pointer.block) := do!
	let ps := !adjust.pointer pointer.block (0 - os.ps) ~
	!assign ps* := type ps.block {
		size: numberp:number max (os.ps, size0),
		next: alloc.free.chain,
	} ~
	!assign alloc.free.chain := ps ~
	{}
~

define !alloc.mut.number init := do!
	type init = number ~
	let size := size.of init ~
	let address := !alloc size ~
	let pointer := pointer:pointer address ~
	!assign pointer* := init ~
	{
		destroy {} := !dealloc (size, address) ~
		get {} := do! pointer* ~
		set v1 := do! (!assign pointer* := v1 ~ {}) ~
	}
~

{
	!alloc,
	!alloc.mut.number,
	!dealloc,
	!mmap,
	!munmap,
}
