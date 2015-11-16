"use strict";

var frp = () => {
	var receivers = [];
	var fire_ = data => { for (var i = 0; i < receivers.length; i++) receivers[i](data); };
	var redirect_ = tf => {
		var frp1 = frp();
		register_(data => tf(data, frp1));
		return frp1;
	};
	var register_ = receiver => receivers.push(receiver);
	return {
		append: frp_ => {
			var frp1 = frp();
			register_(frp1.fire);
			frp_.register(frp1.fire);
			return frp1;
		},
		close: () => receivers = [], // for garbage collection
		concatmap: f => redirect_((data, frp1) => f(data).register(frp1.fire)),
		delay: time => redirect_((data, frp1) => setTimeout(() => frp1.fire(data), time)),
		edge: () => {
			var data_;
			return redirect_((data, frp1) => {
				if(data != data_) frp1.fire(data);
				data_ = data;
			});
		},
		filter: f => redirect_((data, frp1) => { if (f(data)) frp1.fire(data); }),
		fire: fire_,
		fold: (f, value) => redirect_((data, frp1) => frp1.fire(value = f(value, data))),
		last: () => {
			var data_;
			register_(data => data_ = data);
			return () => data_;
		},
		map: f => redirect_((data, frp1) => frp1.fire(f(data))),
		merge: (frp_, f) => {
			var v0, v1;
			var frp1 = frp();
			var fire1 = () => frp1.fire(f(v0, v1));
			register_(data => { v0 = data; fire1(); });
			f.register(data => { v1 = data; fire1(); });
			return frp1;
		},
		redirect: redirect_,
		register: register_,
		resample: frp_ => {
			var data_;
			register_(data => data_ = data);
			return frp_.redirect_((data, frp1) => frp1.fire(data_));
		},
		unique: () => {
			var list = [];
			return redirect_((data, frp1) => {
				if (!read(list).fold((b_, d) => b_ || e == data, false)) {
					frp1.fire(data);
					list.push(data);
				}
			});
		},
	};
};

var frpkbkeys = {};
var frpmb = frp();
var frpmm = frp();
var frpmotion = frp();
var frpori = frp();

var pressed_ = (e, down) => {
	var frp = frpkbkeys[(!(e.which)) ? e.keyCode : (e.which ? e.which : 0)];
	if (frp) frp.fire(down);
};

document.onkeydown = e => pressed_(e, true);
document.onkeyup = e => pressed_(e, false);
document.onmousedown = e => frpmb.fire(true);
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
	frpmm.fire({ x: x, y: y });
};
document.onmouseup = e => frpmb.fire(false);

if (window.DeviceMotionEvent) {
	window.addEventListener("devicemotion", e => {
		frpori.fire({
			a: e.acceleration,
			aig: e.accelerationIncludingGravity,
			rr:e.rotationRate,
			interval: e.interval,
		});
	}, false);
}
else log("device motion not supported");

if (window.DeviceOrientationEvent) {
	window.addEventListener("deviceorientation", e => {
		frpori.fire({
			lr: e.gamma, // the left-to-right tilt in degrees, where right is positive
			fb: e.beta, // the front-to-back tilt in degrees, where front is positive
			dir: e.alpha, // the compass direction the device is facing in degrees
		});
	}, false);
}
else log("device orientation not supported");

var frpanimframe = () => {
	var frp_ = frp();
	var tick = () => {
		frp_.fire(true);
		requestAnimationFrame(tick);
	}
	requestAnimationFrame(tick);
	return frp_;
};

var frpkbpressed = keycode => {
	var frp_;
	if (!(frp_ = frpkbkeys[keycode])) frp_ = frpkbkeys[keycode] = frp();
	return frp_;
};

var frpkbdown = frpkbpressed(40).map(d => d ? 1 : 0);
var frpkbleft = frpkbpressed(37).map(d => d ? -1 : 0);
var frpkbright = frpkbpressed(39).map(d => d ? 1 : 0);
var frpkbup = frpkbpressed(38).map(d => d ? -1 : 0);

var frpkbarrowx = frpkbleft.append(frpkbright); // .fold((a, b) => a + b, 0).last();
var frpkbarrowy = frpkbup.append(frpkbdown); // .fold((a, b) => a + b, 0).last();
var frpmousebutton = frpmb;
var frpmousemove = frpmm;

var frptick = timeout => {
	var frp_ = frp();
	var tick = () => {
		frp_.fire(true);
		setTimeout(tick, timeout);
	}
	setTimeout(tick, timeout);
	return frp_;
};
