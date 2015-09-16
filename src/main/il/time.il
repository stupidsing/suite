declare time-yield = function [] (
	svc-service [svc-halt_, 0, 0,];
);

declare time-sleep = function [ms,] (
	declare ticks1 = svc-service [svc-ticks, 0, 0,] + ms / 10;
	while (svc-service [svc-ticks, 0, 0,] < ticks1) do (
		time-yield [];
	);
);
