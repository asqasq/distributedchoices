package asq.choices.server;

import java.io.IOException;
import java.net.Socket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import asq.choices.ZeroKnowledgeChoice;
import asq.choices.common.NetworkUtilities;

public class ProtocolHandler extends Thread {

	Socket socket;
	Users users;
	boolean loggedIn = false;
	private NetworkUtilities n;

	public ProtocolHandler(Socket socket, Users users) throws Exception {
		this.socket = socket;
		this.users = users;
		this.n = new NetworkUtilities();
	}
	public void run() {
		boolean running = true;
		byte[] buffer = new byte[8192];
		int anzahl;

		do {
			try {
				anzahl = socket.getInputStream().read(buffer);
				if (anzahl < 1) {
					users.removeUser(socket);
					running = false;
					break;
				}
				n.addReceivedBytes(buffer, anzahl);

				String json = n.getNextBuffer();
				while (json != null) {
					//String json = new String(buffer, 0, anzahl);
					System.out.println("Server received:\n" + json);
					JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
					String type = jsonObject.get("type").getAsString();
					System.out.println("Server received type " + type);
					if (type.equals("userentry")) {
						users.loginUser(json, socket);
					} else if (type.equals("printusers")) {
						users.printUsers();
					} else if (type.equals("generateNewChoice")) {
						users.generateNewChoice(json);
					} else if (type.equals("forwardValues")) {
						users.forwardValues(json);
					} else if (type.equals("commitChoice")) {
						users.commitChoice(json);
					}
					json = n.getNextBuffer();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				users.removeUser(socket);
				running = false;
			}
		} while(running);
		try {
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Could not close the socket");
		}
	}
}
