package br.com.betogontijo.sbgreader;

public class PerformanceMonitor extends Thread {

	SbgDataSource dataSource = SbgDataSource.getInstance();

	private volatile boolean running = true;

	PerformanceMonitor() {
	}

	@Override
	public void run() {
		double speed = 0;
		while (running) {
			try {
				int lastMin = dataSource.getDocIdCounter();
				int atualSec = lastMin;
				for (int i = 0; i < 60; i++) {
					int lastSec = atualSec;
					Thread.sleep(1000);
					atualSec = dataSource.getDocIdCounter();
					speed = ((atualSec - lastSec) + speed) / 2;
					System.out.printf("\rDocuments/second: %.2f, Queue Buffer Size: %d        ", speed,
							dataSource.getReferencesBufferQueue().size());
				}
				speed = (atualSec - lastMin) / 60;

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void cancel() {
		running = false;
	}
}
