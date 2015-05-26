package cloud;

import java.util.List;

public class InsertThread extends Thread {

	private MyCountDown threadCounter;

	private Repository repo;

	private List<String> strData;

	public InsertThread(String threadName, MyCountDown threadCounter,
			Repository repo, List<String> strData) {

		super(threadName);

		this.threadCounter = threadCounter;
		this.repo = repo;
		this.strData = strData;
	}

	public void run() {

		System.out.println(getName() + " is running!");
		
		for (int i = 0; i < strData.size(); i++) {

			this.repo.insert(strData.get(i));
		}

		System.out.println(getName() + " is finished!");
		threadCounter.countDown();
	}
}
