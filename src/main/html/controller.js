"use strict";

var log = function(m) {
	var text = document.getElementById("log").value + m + "\n";
	if (text.length > 256) text = text.substring(text.length - 256, text.length);
	document.getElementById("log").value = text;
	// alert(m);
};

var controller = function(canvas, keyboard, mouse, objects) {
	var width = canvas.width, height = canvas.height;

	var context = canvas.getContext("2d");

	var repaint = function(context, objects) {
		context.fillStyle = "#777777";
		context.fillRect(0, 0, width, height);

		context.fillStyle = "#000000";
		context.font = "10px Helvetica";
		context.fillText("Demo", 16, height - 16);

		read(objects).foreach(function(object) { object.draw(context); });
	};

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

				repaint(context, objects);
			}
		}
	};
};
