is-member :=
	list => item =>
		tree / list, (
			item = left / list;
			is-member / (right / list) / item
		)
#

join := f1 => f2 => in => (f1 / (f2 / in)) #

fold :=
	func => list => (
		l => r => switch (
			(tree / r => func / l / (fold / func / r))
			l
	) / (left / list) / (right / list)
#

map := func => list =>
	if (tree / list) then
		((func / (left / list)), (fold / (right / list)))
	else
		$
#
