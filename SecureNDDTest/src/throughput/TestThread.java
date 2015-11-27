package throughput;

import it.unisa.dia.gas.jpbc.Element;

import java.util.List;
import java.util.Map;

import base.MyCounter;
import cloud.MyCountDown;
import cloud.Repository;
import cloud.Repository2;
import secure.PRF;
import secure.AESCoder;

public class TestThread extends Thread {

	private MyCountDown threadCounter;

	private List<Element> tArray;

	private Repository2 repo;
	
	private MyCounter throughput;
	
	private Integer uid;
	
	private Integer repoNum;
	
	private long stTime;

	public TestThread(String threadName, MyCountDown threadCounter, int uid, List<Element> tArray, Repository2 repo, int repoNum, MyCounter throughput, long stTime) {

		super(threadName);

		this.threadCounter = threadCounter;
		this.tArray = tArray;
		this.repo = new Repository2(repo);
		this.throughput = throughput;
		
		this.uid = uid;
		this.repoNum = repoNum;
		this.stTime = stTime;
	}

	public void run() {

		System.out.println(getName() + " is running!");
		
		repo.secureSearch(uid, tArray, repoNum, throughput, stTime);
		
		System.out.println("inner throughput = " + throughput.getCtr());

		//System.out.println(getName() + " is finished! Number of candidate: " + resultInL.size());
		threadCounter.countDown();
	}
}
