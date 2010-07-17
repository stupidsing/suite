concat :-
	list1 => list2 => switch
		(tree list1 => form-tree / (left list1) / list2)
		list2 
#

concat-lists :- fold / concat #

member-of :-
	item => list =>
		join
			/ (fold / and)
			/ (map / (e => e = item))
#

and :- b1 => b2 => if b1 then b2 else false #

or :- b1 => b2 => if b1 then true else b2 #	

fold :-
	func => list => (
		l => r => switch
			(tree r => func / l / (fold / func / r))
			l
	) / left list / right list
#

map :- func => list =>
	if (tree list) then (
		form-tree
			/ (func / left list)
			/ (map / func / right list)
	) else
		()
#

join :- f1 => f2 => in => (f1 / (f2 / in)) #

form-tree :- l => r => l, r #
