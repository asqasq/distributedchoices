package asq.choices.analysis;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Writer;

import com.google.gson.Gson;

import asq.choices.common.WriteResult;

public class Analysis {

	public static void main(String[] args) throws Exception {
		Gson g = new Gson();
		String[] fileNames = {"user01_results.txt", "user02_results.txt", "user03_results.txt", "user04_results.txt", "user05_results.txt", "user06_results.txt"};
		boolean[] idxs = new boolean[fileNames.length];
		BufferedReader[] fin = new BufferedReader[fileNames.length];

		int dupCnt = 0;
		int errSelf = 0;
		int errSelfId = 0;
		int errNotChoosen = 0;

		int nrTests = 0;

		for (int i = 0; i < fin.length; i++) {
			fin[i] = new BufferedReader(new InputStreamReader(new FileInputStream(fileNames[i])));
		}

		boolean running = true;
		while(running) {
			for (int i = 0; i < idxs.length; i++) {
				idxs[i] = false;
			}

			for (int i = 0; i < fin.length; i++) {
				String inputLine = fin[i].readLine();
				if ((inputLine == null) || (inputLine.equals(""))) {
					running = false;
					break;
				}
				if (!running) {
					break;
				}

				WriteResult wr = g.fromJson(inputLine, WriteResult.class);
				if (wr.myName.equals(wr.myChoice)) {
					System.out.println("I han mi selber gwaehtl");
					errSelf++;
				}
				if (wr.myId == wr.myChoiceId) {
					System.out.println("I han mini eigeni ID gwaehtl");
					errSelfId++;

				}
				if (idxs[wr.myChoiceId]) {
					System.out.println("Duplikat!!!");
					dupCnt++;
				} else {
					idxs[wr.myChoiceId] = true;
				}
			}
			if (running) {
				for (int i = 0; i < idxs.length; i++) {
					if (!idxs[i]) {
						System.out.println("Eine Person wurde nicht gewaehlt!! Zeile: " + nrTests);
						errNotChoosen++;
					}
				}
				nrTests++;
			}
		}

		System.out.println(nrTests + " Tests ausgefuehrt. Statistik:\n\tDuplikate: " + dupCnt
				+ "\teigenen Namen gewaehlt: " + errSelf
				+ "\teigene ID gewaehlt: " + errSelfId
				+ "\tPerson NICHT gewaehlt: " + errNotChoosen);
		for (int i = 0; i < fin.length; i++) {
			fin[i].close();
		}

	}

}
