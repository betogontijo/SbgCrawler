package br.com.betogontijo.sbgcrawler;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SbgThreadPoolExecutor extends ThreadPoolExecutor {

	public SbgThreadPoolExecutor(int threadNumber) {
		super(threadNumber, threadNumber, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
	}

}
