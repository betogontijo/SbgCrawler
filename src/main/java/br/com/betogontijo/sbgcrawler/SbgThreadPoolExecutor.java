package br.com.betogontijo.sbgcrawler;

import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

@Service
public class SbgThreadPoolExecutor extends ThreadPoolExecutor {

	static int threadNumber = 1;

	{
		try {
			Properties properties = new Properties();
			properties.load(ClassLoader.getSystemResourceAsStream("sbgcrawler.properties"));
			threadNumber = Integer.parseInt(properties.getProperty("environment.threads"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public SbgThreadPoolExecutor() {
		super(threadNumber, threadNumber, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
	}

}
