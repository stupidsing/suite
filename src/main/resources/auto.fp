concat-lists :- fold {concat} #

concat :-
	list1 => list2 =>
		tree {list1}
		?	form-tree {left {list1}} {list2}
		|	list2
#

member-of :-
	item => join {fold {or}} {map {e => e = item}}
#

tail :-
	list =>
		tree {list}
		?	r = right {list} >>
			concat {r} {tail {r}}
		|	()
#

filter :-
	func => list =>
		tree {list}
		?
			l = left {list} >>
			others = filter {func} {right {list}} >>
			func {l} ? form-tree {l} {others} | others
		|	()
#

fold :-
	func => list =>
		l = left {list} >>
		r = right {list} >>
		tree {r} ? func {l} {fold {func} {r}} | l
#

map :-
	func => list =>
		tree {list}
		?	form-tree {func {left {list}}} {map {func} {right {list}}}
		|	()
#

join :- f1 => f2 => in => f1 {f2 {in}} #

and :- b1 => b2 => (b1 ? b2 | false) #

or :- b1 => b2 => (b1 ? true | b2) #	

form-tree :- l => r => (l, r) #
