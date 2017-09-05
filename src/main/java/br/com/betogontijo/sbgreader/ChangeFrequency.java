package br.com.betogontijo.sbgreader;

public enum ChangeFrequency {
	MINUTELY(60 * 1000), HOURLY(MINUTELY.getFrequency() * 60), DAILY(HOURLY.getFrequency() * 24), WEEKLY(
			DAILY.getFrequency() * 7), MONTHLY(
					DAILY.getFrequency() * 30), YEARLY(MONTHLY.getFrequency() * 12), NEVER(-1);

	private int frequency;

	ChangeFrequency(int frequency) {
		this.frequency = frequency;
	}

	int getFrequency() {
		return this.frequency;
	}
}
