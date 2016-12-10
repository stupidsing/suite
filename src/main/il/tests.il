declare test = function [] (
	declare clazz = new gc-class (
		size = 32,
		+get-pointers = function [stack, p,] ( 0; ),
	);
	log-dword "object = " (gc-allocate [& clazz,]);
	log-dword "object = " (gc-allocate [& clazz,]);
	log-dword "object = " (gc-allocate [& clazz,]);
	log-dword "object = " (gc-allocate [& clazz,]);
	log-dword "object = " (gc-allocate [& clazz,]);
	log-dword "object = " (gc-allocate [& clazz,]);
	log-dword "object = " (gc-allocate [& clazz,]);
	log-dword "object = " (gc-allocate [& clazz,]);
	declare root = null;
	gc-gc [root,];
	log-dword "object = " (gc-allocate [& clazz,]);
);
