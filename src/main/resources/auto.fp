concat-lists :- fold {concat} #

concat :-
	list1 => list2 =>
	(
		tree {list1} => form-tree {left {list1}} {list2}
	|	list2
	)
#

member-of :-
	item =>
		join {fold {or}} {map {e => e = item}}
#

tail :-
	list =>
	(
		tree {list} =>
			(
				r => concat {r} {tail {r}}
			) {right {list}}
	|	()
	)
#

filter :-
	func => list =>
	(
		tree {list} =>
		(
			(
				l => others => (
					func {l} => form-tree {l} {others}
					|	others
				)
			) {left {list}} {filter {func} {right {list}}}
		)
	|	()
	)
#

fold :-
	func => list =>
	(
		l => r =>
			(
				tree {r} => func {l} {fold {func} {r}}
			|	l
			)
	)
	{left {list}}
	{right {list}}
#

map :- func => list =>
	(
		tree {list} =>
			form-tree
				{func {left {list}}}
				{map {func} {right {list}}}
	|	()
	)
#

join :- f1 => f2 => in => f1 {f2 {in}} #

and :- b1 => b2 => (b1 => b2 | false) #

or :- b1 => b2 => (b1 => true | b2) #	

form-tree :- l => r => (l, r) #
