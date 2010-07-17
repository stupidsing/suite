concat :-
	list1 => list2 =>
		(tree list1 => form-tree / (left / list1) / list2)
		(list2) 
#

concat-lists :- fold / concat #

member-of :-
	item =>
		join
			/ (fold / or)
			/ (map / (e => e = item))
#

fold :-
	func => list => (
		l => r =>
			(tree / r => func / l / (fold / func / r))
			(l)
	)
	/ (left / list)
	/ (right / list)
#

map :- func => list =>
	(
		tree / list =>
			form-tree
				/ (func / (left / list))
				/ (map / func / (right / list))
	)
	()
#

and :- b1 => b2 => (b1 => b2) (false) #

or :- b1 => b2 => (b1 => true) (b2) #	

join :- f1 => f2 => in => (f1 / (f2 / in)) #

form-tree :- l => r => l, r #
