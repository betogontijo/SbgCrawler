package br.com.betogontijo.sbgcrawler;

public class PerformanceMonitor extends Thread {

	SbgDataSource dataSource = SbgDataSource.getInstance();

	private volatile boolean running = true;

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		double rate = 0;
		double topTps = 0;
		while (running) {
			try {
				int lastMin = dataSource.getDocIdCounter();
				int atualSec = lastMin;
				for (int i = 0; i < 60; i++) {
					int lastSec = atualSec;
					Thread.sleep(1000);
					atualSec = dataSource.getDocIdCounter();
					rate = ((atualSec - lastSec) + rate) / 2;
					if (rate > topTps) {
						topTps = rate;
					}
					System.out.printf("Rate: %.2fDoc/s, MaxRate: %.2fDoc/s, TotalDocs: %d, QueueBufferSize: %d\r", rate,
							topTps, atualSec, dataSource.getReferencesBufferQueue().size());
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 */
	public void cancel() {
		running = false;
	}
}
