package br.com.betogontijo.sbgreader;

public class PerformanceMonitor extends Thread {

	SbgDataSource dataSource = SbgDataSource.getInstance();

	private volatile boolean running = true;

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		double rateSec = 0;
		double rateMin = 0;
		while (running) {
			try {
				int lastMin = dataSource.getDocIdCounter();
				int atualSec = lastMin;
				for (int i = 0; i < 60; i++) {
					int lastSec = atualSec;
					Thread.sleep(1000);
					atualSec = dataSource.getDocIdCounter();
					rateSec = ((atualSec - lastSec) + rateSec) / 2;
					System.out.printf(
							"\rDocuments/second: %.2f, Documents/minute: %.2f, Documents: %d, Queue Buffer Size: %d",
							rateSec, rateMin, atualSec, dataSource.getReferencesBufferQueue().size());
				}
				rateMin = ((atualSec - lastMin) + rateMin) / 61;

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
