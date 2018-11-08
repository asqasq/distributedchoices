package asq.choices.client;

import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchProviderException;

import org.bouncycastle.openpgp.PGPException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import asq.choices.ZeroKnowledgeChoice;
import asq.choices.common.NetworkUtilities;
import asq.choices.server.Users;

public class ProtocolHandler extends Thread {
	Socket socket;
	boolean loggedIn = false;
	Client c;
	NetworkUtilities n;

	public ProtocolHandler(Socket socket, Client c, NetworkUtilities n) throws Exception {
		this.socket = socket;
		this.c = c;
		this.n= n;
		start();
	}
	public void run() {
		boolean running = true;
		byte[] buffer = new byte[8192];
		int anzahl;

		do {
			try {
				anzahl = socket.getInputStream().read(buffer);
				if (anzahl < 1) {
					running = false;
					break;
				}
				n.addReceivedBytes(buffer, anzahl);

				String json = n.getNextBuffer();
				while (json != null) {
					running = false;
					//				String json = new String(buffer, 0, anzahl);
					System.out.println("Server received:\n" + json);
					JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
					String type = jsonObject.get("type").getAsString();
					System.out.println("Server received type " + type);
					if (type.equals("userlist")) {
						c.setUserList(json);
					} else if (type.equals("generateNewChoice")) {
						c.generateNewChoiceHandler();
					} else if (type.equals("forwardValues")) {
						c.forwardValuesHandler(json);
					} else if (type.equals("commitChoice")) {
						c.commitChoiceHandler(json);
					}
					json = n.getNextBuffer();
					running = true;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				running = false;
				c.errorHandler(e);
			} catch (NoSuchProviderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				running = false;
				c.errorHandler(e);
			} catch (PGPException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				running = false;
				c.errorHandler(e);
			}
		} while(running);
		try {
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Could not close the socket");
		}
		c.socketClosed();
	}
}
