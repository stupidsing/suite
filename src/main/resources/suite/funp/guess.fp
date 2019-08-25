define io := consult "io.fp" ~

define !get.number := io/!get.number ~
define !put.line := io/!put.line ~
define !put.number := io/!put.number ~
define !put.string := io/!put.string ~

consult "linux.fp" ~

define !guess () := do!
	let answer := !asm.rdtscp and +x7FFFFFFF % 100 ~
	!put.string "I am telling you, it is " ~
	!put.number answer ~
	!put.line () ~
	!put.string "hello, please guess%0A" ~
	fold (
		guess := !get.number () #
		guess != answer #
		if (guess < answer) then (
			!put.string "higher...%0A" ~
			!get.number ()
		) else if (answer < guess) then (
			!put.string "lower...%0A" ~
			!get.number ()
		) else (
			error
		) #
		!put.string "you got it, it is " ~
		!put.number answer ~
		!put.line () ~
		0
	)
~

{
	!guess,
}
