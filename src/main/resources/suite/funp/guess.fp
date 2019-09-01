define.global io := consult "io.fp" ~

define.global {
	!get.number,
	!put.line,
	!put.number,
	!put.string,
} := io ~

consult "linux.fp" ~

define.global !guess () := do!
	let answer := !asm.rdtscp and +x7FFFFFFF % 100 ~
	--!put.string "I am telling you, it is " ~
	--!put.number answer ~
	--!put.line () ~
	!put.string "hello, please guess%0A" ~
	fold (
		(n, guess) := (1, !get.number ()) #
		guess != answer #
		!! if (guess < answer) then (
			!put.string "higher...%0A"
		) else if (answer < guess) then (
			!put.string "lower...%0A"
		) else (
			error
		) ~
		(n + 1, !get.number ()) #
		!put.string "you got it in " ~
		!put.number n ~
		!put.string " guesses!%0A" ~
		0
	)
~

{
	!guess,
}
