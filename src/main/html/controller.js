"use strict";

var log = function(m) {
	var text = document.getElementById("log").value + m + "\n";
	if (text.length > 256) text = text.substring(text.length - 256, text.length);
	document.getElementById("log").value = text;
	// alert(m);
};

var controller = function(canvas, keyboard, mouse, objects) {
	return {
		tick: function() {
			if (!keyboard.paused()) {
				objects = read(objects)
					.map(function(object) {
						object.input(keyboard, mouse);
						object.move();
						return object.spawn();
					})
					.concat()
					.list();
			}
		}
	};
};
