package br.com.betogontijo.sbgcrawler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentSetQueue<E> extends LinkedHashSet<E> implements Queue<E> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7906542722835397462L;

	final java.util.concurrent.locks.ReentrantLock lock = new ReentrantLock();

	public E element() {
		return iterator().next();
	}

	public boolean offer(E arg0) {
		return add(arg0);
	}

	public E peek() {
		try {
			return element();
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	public E poll() {
		return remove();
	}

	public boolean addAll(Collection<? extends E> c) {
		lock.lock();
		try {
			boolean modified = false;
			for (E e : c)
				if (add(e))
					modified = true;
			return modified;
		} finally {
			lock.unlock();
		}
	}

	public E remove() {
		lock.lock();
		try {
			E next = peek();
			if (next != null) {
				remove(next);
			}
			return next;
		} finally {
			lock.unlock();
		}
	}

	public List<E> removeMany(int count) {
		List<E> nextList = new ArrayList<E>();
		lock.lock();
		try {
			while (count > 0) {
				E next = element();
				nextList.add(next);
				remove(next);
				count--;
			}
			return nextList;
		} finally {
			lock.unlock();
		}
	}
}
