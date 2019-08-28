define io := consult "io.fp" ~

define {
	!get.number,
	!put.line,
	!put.number,
} := io ~

define !put.string := io/!put.string ~

consult "linux.fp" ~

define !guess () := do!
	let answer := !asm.rdtscp and +x7FFFFFFF % 100 ~
	!put.string "I am telling you, it is " ~
	!put.number answer ~
	!put.line () ~
	!put.string "hello, please guess%0A" ~
	fold (
		(n, guess) := (1, !get.number ()) #
		guess != answer #
		if (guess < answer) then (
			!put.string "higher...%0A" ~
			(n + 1, !get.number ())
		) else if (answer < guess) then (
			!put.string "lower...%0A" ~
			(n + 1, !get.number ())
		) else (
			error
		) #
		!put.string "you got it in " ~
		!put.number n ~
		!put.string " guesses!%0A" ~
		0
	)
~

{
	!guess,
}
