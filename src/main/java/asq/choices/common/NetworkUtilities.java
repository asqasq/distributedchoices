package asq.choices.common;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class NetworkUtilities {
	final static int RINGBUFFER_SIZE = 1 * 1024 * 1024;

	private byte[] ringBuffer;
	private int head;
	private int tail;
	private int size;

	public NetworkUtilities() {
		ringBuffer = new byte[RINGBUFFER_SIZE];
		head = 0;
		tail = 0;
		size = 0;
	}

	public synchronized void addReceivedBytes(byte[] b) throws RuntimeException {
		if (b.length > (RINGBUFFER_SIZE - size - 1)) {
			throw new RuntimeException("Ringbuffer is full");
		}
		for (int i = 0; i < b.length; i++) {
			ringBuffer[tail] = b[i];
			tail = (tail + 1) % RINGBUFFER_SIZE;
			size++;
		}
	}

	public synchronized void addReceivedBytes(byte[] b, int s) throws RuntimeException {
//		int maxs = Math.max(b.length, s);
		int maxs = s;
		if (maxs > (RINGBUFFER_SIZE - size - 1)) {
			throw new RuntimeException("Ringbuffer is full");
		}
		for (int i = 0; i < maxs; i++) {
			ringBuffer[tail] = b[i];
			tail = (tail + 1) % RINGBUFFER_SIZE;
			size++;
		}
	}

	public synchronized String getNextBuffer() {
		int i = head;
		int s = 0;

		while ((i != tail) && (ringBuffer[i] != 0)) {
			i = (i + 1) % RINGBUFFER_SIZE;
			s++;
		}
		if (i == tail) {
			return null; //no full block found
		}

		byte[] res = new byte[s];

		for (int k = 0; k < res.length; k++) {
			res[k] = ringBuffer[head];
			head = (head + 1) % RINGBUFFER_SIZE;
			size--;
		}
		head = (head + 1) % RINGBUFFER_SIZE;
		size--;
		return new String(res);
	}

	public void sendString(Socket socket, String json) throws IOException {
		OutputStream out = socket.getOutputStream();
		out.write((json + '\0').getBytes());
		out.flush();
	}

	public void sendString(OutputStream out, String json) throws IOException {
		out.write((json + '\0').getBytes());
		out.flush();
	}

	public void sendString(OutputStream out, byte[] b) throws IOException {
		out.write(b);
		out.write(0);
		out.flush();
	}

	public static void main(String[] args) throws Exception {
		String a = "hallo";
		String b = "du";
		String c = "was";
		String d = "1234567890";
		String e = "DasIschALangsWortFuerDaTest";
		String f = "IBruchaDefinitivNomolALangsWort";

		NetworkUtilities n = new NetworkUtilities();

		for (int k = 0; k < 100; k++) {
			n.addReceivedBytes((a + '\0').getBytes());
			n.addReceivedBytes((b + '\0').getBytes());
			n.addReceivedBytes((c + '\0').getBytes());
			n.addReceivedBytes((d + '\0').getBytes());
			n.addReceivedBytes((e + '\0').getBytes());
			n.addReceivedBytes((f + '\0').getBytes());

			for (int i = 0; i < 10; i++) {
				String s = n.getNextBuffer();
				if (s != null) {
					System.out.println("Nr " + i + ": " + s);
				} else {
					System.out.println("Leer nr " + i);
				}
			}
		}
	}
}
