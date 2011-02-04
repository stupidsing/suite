package org.suite.node;

import java.util.concurrent.atomic.AtomicInteger;

public class Reference extends Node {

	private Node node = this;
	private final int id = counter.getAndIncrement();

	private static final AtomicInteger counter = new AtomicInteger();

	public void bound(Node node) {
		this.node = node;
	}

	public void unbound() { // Happens during backtracking
		node = this;
	}

	@Override
	public Node finalNode() {
		return (node != this) ? node.finalNode() : node;
	}

	public int getId() {
		return id;
	}

}
