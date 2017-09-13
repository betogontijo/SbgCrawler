package br.com.betogontijo.sbgcrawler;

import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentSetQueue<E> extends LinkedHashSet<E> implements Queue<E> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7906542722835397462L;

	final java.util.concurrent.locks.ReentrantLock lock = new ReentrantLock();

	/* (non-Javadoc)
	 * @see java.util.Queue#element()
	 */
	@Override
	public E element() {
		return iterator().next();
	}

	/* (non-Javadoc)
	 * @see java.util.Queue#offer(java.lang.Object)
	 */
	@Override
	public boolean offer(E arg0) {
		return add(arg0);
	}

	/* (non-Javadoc)
	 * @see java.util.Queue#peek()
	 */
	@Override
	public E peek() {
		try {
			return element();
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see java.util.Queue#poll()
	 */
	@Override
	public E poll() {
		return remove();
	}

	/* (non-Javadoc)
	 * @see java.util.Queue#remove()
	 */
	@Override
	public E remove() {
		lock.lock();
		try {
			E next = element();
			remove(next);
			return next;
		} finally {
			lock.unlock();
		}
	}
}
