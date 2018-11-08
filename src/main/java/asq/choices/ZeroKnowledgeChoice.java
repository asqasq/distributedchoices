package asq.choices;

import java.security.Security;
import java.util.Random;

import asq.choices.client.Client;
import asq.choices.client.gui.MainWindow;

public class ZeroKnowledgeChoice {
	final static String[] names = {"user01", "user02", "user03", "user04", "user05", "user06"};
	volatile int myIdx = -1;
	volatile int myChoice = -1;
	volatile int startValueQ;
	volatile int expectedValueQ;
	volatile int startValueSum;
	volatile int expectedValueSum;

	public static void main(String[] args) throws Exception {
	Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

		int errcnt = 0;
		long nr_test_loops = 1000000000;
		for (long i = 0; i < nr_test_loops; i++) {
			if ((i % 1000000) == 0) {
				System.out.println("Tests: " + i + ", Fehler bis jetzt: " + errcnt);
			}
			errcnt += testDuplicates(i == (nr_test_loops - 1), names.length);
		}
		System.out.println("=======\nTotale Anzahl Fehler = " + errcnt + "\n=======");
	}

	public ZeroKnowledgeChoice(int myIdx) {
		this.myIdx = myIdx;
	}

	public void setMyId(int myId) {
		this.myIdx = myId;
	}

	public void generateNumber(int nrClients) {
		Random r = new Random();

		do {
//			myChoice = r.nextInt(names.length);
			myChoice = r.nextInt(nrClients);
		} while (myChoice == myIdx);

	}

	public int getMyChoice() {
		return myChoice;
	}

	public int startRoundValueQ(int nrClients) {
		Random r = new Random();
		startValueQ = r.nextInt(1000);
		expectedValueQ = 0;
		for (int i = 0; i < nrClients; i++) {
			expectedValueQ += (i * i);
		}
		return startValueQ;
	}
	public int startRoundValueSum(int nrClients) {
		Random r = new Random();
		startValueSum = r.nextInt(1000);
		expectedValueSum = 0;
		for (int i = 0; i < nrClients; i++) {
			expectedValueSum += (i);
		}
		return startValueSum;
	}
	public int forwardNumberSum(int inputNumber) {
		return (inputNumber + myChoice);
	}
	public int forwardNumberQ(int inputNumber) {
		return (inputNumber + myChoice * myChoice);
	}
	public boolean endRoundQ(int inputNumber) {
//		System.out.println("input: " + inputNumber + ", startQ: " + startValueQ + ", expected valueQ: " + expectedValueQ);;
		if ((inputNumber - startValueQ) == expectedValueQ) {
//			System.out.println("YES");
			return true;
		} else {
//			System.out.println("NAI");
			return false;
		}
	}
	public boolean endRoundSum(int inputNumber) {
//		System.out.println("input: " + inputNumber + ", startSum: " + startValueSum + ", expected value SUM: " + expectedValueSum);;
		if ((inputNumber - startValueSum) == expectedValueSum) {
//			System.out.println("YES");
			return true;
		} else {
//			System.out.println("NAI");
			return false;
		}
	}
	public String toString() {
		return ("Ich bin " + names[myIdx] + "(idx " + myIdx + ") und habe "
				+ names[myChoice] + " (idx " + myChoice + ") geaehlt."
			   );
	}

	public static boolean forwardTestRound(ZeroKnowledgeChoice[] pers) {
		int start = pers[0].startRoundValueQ(names.length);
		int inputNumber = start;
//		System.out.println("Start: " + start);
		for (int i = 0; i < pers.length; i++) {
			inputNumber = pers[i].forwardNumberQ(inputNumber);
//			System.out.print(inputNumber + ", ");
		}
		boolean test01 = pers[0].endRoundQ(inputNumber);

		start = pers[0].startRoundValueSum(names.length);
		inputNumber = start;
		for (int i = 0; i < pers.length; i++) {
			inputNumber = pers[i].forwardNumberSum(inputNumber);
		}
		boolean test02 = pers[0].endRoundSum(inputNumber);

		boolean res = test01 & test02;

		return (!res);
	}
	public static int testDuplicates(boolean printValues, int nrNames) {
		ZeroKnowledgeChoice pers[] = new ZeroKnowledgeChoice[6];
		boolean[] idxs = new boolean[6];

		for (int i = 0; i < pers.length; i++) {
			pers[i] = new ZeroKnowledgeChoice(i);
		}

		boolean duplikat = true;
		boolean duplikat2 = true;
		int cnt = 0;
		int errcnt = 0;
		while (duplikat) {
			duplikat = false;
			duplikat2 = false;
			for (int i = 0; i < idxs.length; i++) {
				idxs[i] = false;
			}
			for (int i = 0; i < pers.length; i++) {
				pers[i].generateNumber(nrNames);
			}

			duplikat2 = forwardTestRound(pers);
			for (int i = 0; i < pers.length; i++) {
				if (!idxs[pers[i].myChoice]) {
					idxs[pers[i].myChoice] = true;
				} else {
//					System.out.println("DUPLIKAT!!");
					duplikat = true;
				}
			}
			if (duplikat != duplikat2) {
				System.out.println("Check failed: duplikat = " + duplikat + ", duplikat2 = " + duplikat2);
				errcnt++;
//				break;
			}
			cnt++;
		}

		if (printValues) {
			System.out.println("Anzahl Fehler: " + errcnt);;
			System.out.println("Loesung nach " + cnt + " Runden gefunden:");
			for (int i = 0; i < pers.length; i++) {
				System.out.println(pers[i]);
			}
		}

		return errcnt;
	}
}
