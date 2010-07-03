concat =
	list1 => list2 => switch
		(tree / list1 => form-tree / (left / list1) / list2)
		list2 
#

concat-lists = fold / concat #

member-of =
	item => list =>
		join
			/ (fold / (b1 => b2 => b1, b2))
			/ (map / (e => e = item))
#

fold =
	func => list => (
		l => r => switch
			(tree / r => func / l / (fold / func / r))
			l
	) / (left / list) / (right / list)
#

map = func => list =>
	if (tree / list) then (
		form-tree
			/ (func / (left / list))
			/ (fold / (right / list))
	) else
		()
#

join = f1 => f2 => in => (f1 / (f2 / in)) #

form-tree = left => right => p (left, right) #
