package asq.choices.client;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.NoSuchProviderException;
import java.security.Security;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;

import com.google.gson.Gson;

import asq.choices.KeyMgmt;
import asq.choices.ZeroKnowledgeChoice;
import asq.choices.client.gui.MainWindow;
import asq.choices.client.gui.ZKprotoUpdateListener;
import asq.choices.common.CommitChoice;
import asq.choices.common.ForwardValues;
import asq.choices.common.GenerateNewChoice;
import asq.choices.common.NetworkUtilities;
import asq.choices.common.UserEntry;
import asq.choices.common.UserList;
import asq.choices.common.Values;
import asq.choices.common.WriteResult;
import asq.choices.server.ServerMain;

public class Client {

	private Socket socket;

	private String name;
	private String pubKeyFile;
	private String pubKey;
	private volatile UserList ul;
	private Gson g;
	private ProtocolHandler protocolHandler;
	private ZeroKnowledgeChoice zkc;
	private NetworkUtilities n;

	private volatile int nrClients = 0;

	private volatile boolean committed = false;
	private volatile boolean duplikat;

	private volatile Object cond;
	private volatile Object generateRandcond;

	private WriteResult writeResult;
	private File resultsFile;
	private FileOutputStream resultOutputStream;

	private String finalResultFile;

	private KeyMgmt k;
	private ZKprotoUpdateListener zkl = null;

	public static void main(String[] args) throws Exception {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

		if (args.length == 0) {
			MainWindow mw = new MainWindow();
			mw.startGui();
		} else if (args[0].equals("--server")) {
			ServerMain sm = new ServerMain();
			sm.runServer();
		} else if (args[0].equals("--user")) {
			String name = args[1];
			String password = args[2];
			KeyMgmt k = new KeyMgmt(name, password.toCharArray());
			Client client = new Client("localhost", 63425, name, k.getPubKeyFile(), k);
		} else if (args[0].equals("--test")) {
			int nrExpectedClients = Integer.parseInt(args[1]);
			String name = args[2];
			String password = args[3];
			KeyMgmt k = new KeyMgmt(name, password.toCharArray());
			Client client = new Client("localhost", 63425, name, k.getPubKeyFile(), k);

			for (int i = 0; i < 1000; i++) {
				client.runStressTest(nrExpectedClients);
				Thread.sleep(1000);
			}
		} else if (args[0].equals("--help")) {
			usage();
			System.exit(1);
		}
//		if (args.length == 1) {
//			Main m = new Main(Integer.parseInt(args[0]));
//		} else {
////			int errcnt = 0;
////			long nr_test_loops = 1000000000;
////			for (long i = 0; i < nr_test_loops; i++) {
////				if ((i % 1000000) == 0) {
////					System.out.println("Tests: " + i + ", Fehler bis jetzt: " + errcnt);
////				}
////				errcnt += testDuplicates(i == (nr_test_loops - 1));
////			}
////			System.out.println("=======\nTotale Anzahl Fehler = " + errcnt + "\n=======");
//
//
//			KeyMgmt k = new KeyMgmt("user01", "password".toCharArray());
//			Client client = new Client("localhost", 63425, "user01", k.getPubKeyFile());
//		}
	}

	public static void usage() {
		System.out.println("--server | --user <name> <password> \n\n");
	}


	public Client(String hostname, int port, String name, String pubKeyFile, KeyMgmt k) throws Exception {
		this.k = k;
		socket = new Socket(hostname, port);
		this.pubKeyFile = pubKeyFile;
		this.name = name;

		resultsFile = new File(name + "_results.txt");
		resultOutputStream = new FileOutputStream(resultsFile);
		writeResult = new WriteResult();

		finalResultFile = getResultFileName(name);

		n = new NetworkUtilities();

		cond = new Object();
		generateRandcond = new Object();

		g = new Gson();

		protocolHandler = new ProtocolHandler(socket, this, n);


		readPublicKey();
		login();
		zkc = new ZeroKnowledgeChoice(-1);

	}

	public Client(String hostname, int port, String name, String pubKeyFile, KeyMgmt k, ZKprotoUpdateListener zkl) throws Exception {
		this(hostname, port, name, pubKeyFile, k);
		this.zkl = zkl;
	}

	public static String getResultFileName(String name) {
		return (name + "_finalresult.txt");
	}

	public void login() throws Exception {
		UserEntry ue = new UserEntry(name, pubKey);

		String us = g.toJson(ue);
		System.out.println(us);

		n.sendString(socket, us);
	}

	public void readPublicKey() {
		byte[] b = null;
		try {
			File f = new File(pubKeyFile);
			b = new byte[(int)f.length()];
			FileInputStream fin = new FileInputStream(f);
			fin.read(b);
			fin.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		pubKey = new String(b);
		System.out.println("Pubkey read from file:\n" + pubKey);
	}

	public void sendGenerateChoice() throws Exception {
		GenerateNewChoice gnc = new GenerateNewChoice();

		String us = g.toJson(gnc);
		System.out.println(us);
		n.sendString(socket, us);
	}

	public void commit() throws Exception {
		CommitChoice cc = new CommitChoice();

		String us = g.toJson(cc);
		System.out.println(us);
		n.sendString(socket, us);
	}

	public String encryptValues(int v1, int v2) throws Exception {
		int myId = ul.getMyId(name);

		Values values = new Values(v1, v2);

    	ByteArrayOutputStream bout = new ByteArrayOutputStream();
    	ByteArrayInputStream bin = new ByteArrayInputStream(g.toJson(values).getBytes());

		InputStream input = new ByteArrayInputStream(ul.getUserkey((myId + 1) % nrClients).getBytes());
		PGPPublicKey pubKey = k.readPublicKey(input);

    	k.encryptData(bout, bin, pubKey, true, true);

    	String encValues = bout.toString();
    	return encValues;
	}

	public Values decryptValues(ForwardValues fwv) throws NoSuchProviderException, FileNotFoundException, IOException, PGPException {
    	ByteArrayOutputStream bout = new ByteArrayOutputStream();
    	k.decryptData(new ByteArrayInputStream(fwv.encryptedValues.getBytes()),
    			new FileInputStream(k.getSecretKeyFile()),
    			k.getPassphrase(),
    			"nuet",
    			bout);

    	String dec = bout.toString();
		Values v = g.fromJson(dec, Values.class);
		return v;
	}

	public void runZKprotocolOnce() throws Exception {
		int myId = ul.getMyId(name);
//		zkc = new ZeroKnowledgeChoice(myId);


//		if (myId == 0) {
			duplikat = true;
			int roundNumber = 0;
			while (duplikat) {
				synchronized(generateRandcond) {
					sendGenerateChoice();
					generateRandcond.wait();
				}
				int startQ = zkc.startRoundValueQ(nrClients);
				int startSum = zkc.startRoundValueSum(nrClients);

				int v1 = zkc.forwardNumberQ(startQ);
				int v2 = zkc.forwardNumberSum(startSum);


				ForwardValues fwv =
						new ForwardValues(name, roundNumber, name,
										  ul.getUserName((myId + 1) % nrClients), encryptValues(v1, v2));

				String us = g.toJson(fwv);
				System.out.println(us);
				n.sendString(socket, us);

				synchronized(cond) {
					cond.wait();
				}
				roundNumber++;
			}
			System.out.println("Found solution after " + roundNumber + " rounds.");
			commit();
//		}
	}

	// Handlers
	public void setUserList(String json) {
		ul = g.fromJson(json, UserList.class);
		System.out.println("Client: got userlist:\n");
		ul.printUsers();
		nrClients = ul.getNrUsers();
		int myId = ul.getMyId(name);
		zkc.setMyId(myId);

		if (zkl != null) {
			zkl.listChanged(ul.getUsers());
		}
	}

	public void generateNewChoiceHandler() {
		zkc.generateNumber(nrClients);
		synchronized(generateRandcond) {
			generateRandcond.notify();
		}
	}

	public void forwardValuesHandler(String json) throws NoSuchProviderException, FileNotFoundException, IOException, PGPException {
		ForwardValues fwv = g.fromJson(json, ForwardValues.class);

		if (!fwv.receiver.equals(name)) {
			return; //ignore, if this is not for me
		}

		if (fwv.sender.equals(name)) {
			return; // ignore message sent by me to myself
		}

		if (fwv.initiator.equals(name)) {
			// I am the initiator and the values came back to me
			// for GUI
			if (zkl != null) {
				zkl.currentRound(fwv.roundNumber);
			}

			endRound(fwv);
		} else {
			// I am not the initiator, so do the computation and forward the values
			// for GUI
			if (zkl != null) {
				zkl.currentRound(fwv.roundNumber);
			}


			Values v;
			try {
				v = decryptValues(fwv);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				v = new Values(0,0);
			}
			int v1 = zkc.forwardNumberQ(v.v1);
			int v2 = zkc.forwardNumberSum(v.v2);
//			fwv.v1 = zkc.forwardNumberQ(fwv.v1);
//			fwv.v2 = zkc.forwardNumberSum(fwv.v2);
			int myId = ul.getMyId(name);
			fwv.receiver = ul.getUserName((myId + 1) % nrClients);
			fwv.sender = name;
			try {
				fwv.encryptedValues = encryptValues(v1, v2);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				fwv.encryptedValues = "";
			}

			String us = g.toJson(fwv);
			System.out.println(us);
			n.sendString(socket, us);
		}
	}

	public void endRound(ForwardValues fwv) throws NoSuchProviderException, FileNotFoundException, IOException, PGPException {
		Values v;
		v = decryptValues(fwv);
//		try {
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			System.out.println("Could not decrypt values...");
//			v = new Values(0,0);
//		}
		boolean test01 = zkc.endRoundQ(v.v1);
		boolean test02 = zkc.endRoundSum(v.v2);
//		boolean test01 = zkc.endRoundQ(fwv.v1);
//		boolean test02 = zkc.endRoundSum(fwv.v2);

		boolean res = test01 & test02;
		duplikat = !res;

		if (duplikat) {
			System.out.println("YEEEESSSSSSS");
		} else {
			System.out.println("NONONONONONO");
		}

		synchronized(cond) {
			cond.notify();
		}
	}

	public void commitChoiceHandler(String json) throws IOException {
		CommitChoice cc = g.fromJson(json, CommitChoice.class);
		int myId = ul.getMyId(name);
		System.out.println("I am " + name + " (id " + myId + "). My final choice is " + ul.getUserName(zkc.getMyChoice()) + ", id " + zkc.getMyChoice());

		String myChoice = ul.getUserName(zkc.getMyChoice());

		writeResult.myName = name;
		writeResult.myId = myId;
		writeResult.myChoice = myChoice;
		writeResult.myChoiceId = zkc.getMyChoice();

		String res = g.toJson(writeResult);

		resultOutputStream.write((res + "\n").getBytes());
		resultOutputStream.flush();

		FileOutputStream frout = new FileOutputStream(finalResultFile);
		frout.write(myChoice.getBytes());
		frout.close();

		// for GUI
		if (zkl != null) {
			zkl.committedChoice(myChoice);
		}
	}

	// error handlers
	public void errorHandler(IOException e) {
		if (zkl != null) {
			zkl.reportError(e.getMessage());
		}
	}
	public void errorHandler(NoSuchProviderException e) {
		if (zkl != null) {
			zkl.reportError(e.getMessage());
		}
	}
	public void errorHandler(PGPException e) {
		if (zkl != null) {
			zkl.pgpError(e.toString());
		}
	}
	public void socketClosed() {
		if (zkl != null) {
			zkl.loggedOut();
		}
	}


	// handlers for GUI
	public String getPubKey(int id) {
		return ul.getUserkey(id);
	}

	// debug
	public void runStressTest(int nrExpectedClients) throws Exception {
		while (nrExpectedClients != nrClients) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		runZKprotocolOnce();
	}
}
