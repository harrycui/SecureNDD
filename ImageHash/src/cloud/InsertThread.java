package cloud;

import java.util.List;

public class InsertThread extends Thread {

	private MyCountDown threadCounter;

	private Repository repo;

	private List<RawRecord> rawRecords;

	public InsertThread(String threadName, MyCountDown threadCounter,
			Repository repo, List<RawRecord> rawRecords) {

		super(threadName);

		this.threadCounter = threadCounter;
		this.repo = repo;
		this.rawRecords = rawRecords;
	}

	public void run() {

		System.out.println(getName() + " is running!");
		
		for (int i = 0; i < rawRecords.size(); i++) {

			this.repo.insert(rawRecords.get(i));
		}

		System.out.println(getName() + " is finished!");
		threadCounter.countDown();
	}
}
