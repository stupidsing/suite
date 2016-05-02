"use strict";

var signal = () => { // FRP dispatcher
	var receivers = [];
	var fire_ = data => { for (var i = 0; i < receivers.length; i++) receivers[i](data); };
	var redirect_ = tf => {
		var signal1 = signal();
		register_(data => tf(data, signal1));
		return signal1;
	};
	var register_ = receiver => receivers.push(receiver);
	return {
		append: signal_ => {
			var signal1 = signal();
			register_(signal1.fire);
			signal_.register(signal1.fire);
			return signal1;
		},
		close: () => receivers = [], // for garbage collection
		concatmap: f => redirect_((data, signal1) => f(data).register(signal1.fire)),
		delay: time => redirect_((data, signal1) => setTimeout(() => signal1.fire(data), time)),
		edge: () => {
			var data_;
			return redirect_((data, signal1) => {
				if(data != data_) signal1.fire(data);
				data_ = data;
			});
		},
		filter: f => redirect_((data, signal1) => { if (f(data)) signal1.fire(data); }),
		fire: fire_,
		fold: (f, value) => redirect_((data, signal1) => signal1.fire(value = f(value, data))),
		last: () => {
			var data_;
			register_(data => data_ = data);
			return () => data_;
		},
		map: f => redirect_((data, signal1) => signal1.fire(f(data))),
		merge: (signal_, f) => {
			var v0, v1;
			var signal1 = signal();
			var fire1 = () => signal1.fire(f(v0, v1));
			register_(data => { v0 = data; fire1(); });
			f.register(data => { v1 = data; fire1(); });
			return signal1;
		},
		read: () => {
			var list = [];
			register_(data => list.push(data));
			return () => {
				var r = read(list);
				list = [];
				return r;
			};
		},
		redirect: redirect_,
		register: register_,
		resample: signal_ => {
			var data_;
			register_(data => data_ = data);
			return signal_.redirect((data, signal1) => signal1.fire(data_));
		},
		unique: () => {
			var list = [];
			return redirect_((data, signal1) => {
				if (!read(list).fold((b_, d) => b_ || e == data, false)) {
					signal1.fire(data);
					list.push(data);
				}
			});
		},
	};
};

var frp = function() {
	var kbkeys = {};
	var mouseclick = signal();
	var mousemove = signal();
	var motion = signal();
	var orientation = signal();

	var pressed_ = (e, down) => {
		var signal = kbkeys[(!(e.which)) ? e.keyCode : (e.which ? e.which : 0)];
		if (signal) signal.fire(down);
	};

	document.onkeydown = e => pressed_(e, true);
	document.onkeyup = e => pressed_(e, false);
	document.onmousedown = e => mouseclick.fire(true);
	document.onmousemove = e => {
		var e1 = (!e) ? window.event : e;
		var x;
		var y;
		if (e1.pageX || e1.pageY) {
			x = e1.pageX;
			y = e1.pageY;
		} else if (e1.clientX || e1.clientY) {
			x = e1.clientX + document.body.scrollLeft + document.documentElement.scrollLeft;
			y = e1.clientY + document.body.scrollTop + document.documentElement.scrollTop;
		}
		mousemove.fire({ x: x, y: y });
	};
	document.onmouseup = e => mouseclick.fire(false);

	if (window.DeviceMotionEvent)
		window.addEventListener("devicemotion", e => {
			motion.fire({
				a: e.acceleration,
				aig: e.accelerationIncludingGravity,
				rr: e.rotationRate,
				interval: e.interval,
			});
		}, false);
	else log("device motion not supported");

	if (window.DeviceOrientationEvent)
		window.addEventListener("deviceorientation", e => {
			orientation.fire({
				lr: e.gamma, // the left-to-right tilt in degrees, where right is positive
				fb: e.beta, // the front-to-back tilt in degrees, where front is positive
				dir: e.alpha, // the compass direction the device is facing in degrees
			});
		}, false);
	else log("device orientation not supported");

	var keypressed = keycode => {
		var signal_;
		if (!(signal_ = kbkeys[keycode])) signal_ = kbkeys[keycode] = signal();
		return signal_;
	};
	var keydown = keypressed(40).map(d => d ? 1 : 0);
	var keyleft = keypressed(37).map(d => d ? -1 : 0);
	var keyright = keypressed(39).map(d => d ? 1 : 0);
	var keyup = keypressed(38).map(d => d ? -1 : 0);

	return {
		animframe: () => {
			var signal_ = signal();
			var tick = () => {
				signal_.fire(true);
				requestAnimationFrame(tick);
			}
			requestAnimationFrame(tick);
			return signal_;
		},
		kb: {
			arrowdown: keydown,
			arrowleft: keyleft,
			arrowright: keyright,
			arrowup: keyup,
			arrowx: keyleft.append(keyright), // .fold((a, b) => a + b, 0).last();
			arrowy: keyup.append(keydown), // .fold((a, b) => a + b, 0).last();
			keypressed: keypressed,
		},
		motion: motion,
		mouse: {
			click: mouseclick,
			move: mousemove,
		},
		orientation: orientation,
		tick: timeout => {
			var signal_ = signal();
			var tick = () => {
				signal_.fire(true);
				setTimeout(tick, timeout);
			}
			setTimeout(tick, timeout);
			return signal_;
		},
	};
} ();
