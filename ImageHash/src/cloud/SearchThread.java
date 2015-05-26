package cloud;

import it.unisa.dia.gas.jpbc.Element;

import java.util.List;

public class SearchThread extends Thread {

	private MyCountDown threadCounter;

	private Repository repo;
	
	private int id;

	private List<Element> query;
	
	private List<Integer> resultOfRepo;

	public SearchThread(String threadName, MyCountDown threadCounter,
			Repository repo, int id, List<Element> query, List<Integer> resultOfRepo) {

		super(threadName);

		this.threadCounter = threadCounter;
		this.repo = repo;
		this.id = id;
		this.query = query;
		this.resultOfRepo = resultOfRepo;
	}

	public void run() {

		System.out.println(getName() + " is running!");
		
		resultOfRepo.addAll(repo.secureSearch(id, query));

		System.out.println(getName() + " is finished! Number of result: " + resultOfRepo.size());
		threadCounter.countDown();
	}
}
