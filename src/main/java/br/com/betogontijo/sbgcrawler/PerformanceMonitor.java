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
		double currentTime = 0;
		Long initialSize = dataSource.getDocIdCounter();
		Long atualSize = initialSize;
		while (running) {
			try {
				Thread.sleep(1000);
				currentTime++;
				atualSize = dataSource.getDocIdCounter();
				rate = (atualSize - initialSize) / currentTime;
				System.out.printf("Rate: %.2fDoc/s, TotalDocs: %d, QueueBufferSize: %d\r", rate, atualSize,
						dataSource.getReferencesBufferQueue().size());

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
