package asq.choices.client.gui;

import java.io.IOException;
import java.security.NoSuchProviderException;

import org.bouncycastle.openpgp.PGPException;

public interface ZKprotoUpdateListener {
	public void committedChoice(String name);
	public void currentRound(int round);
	public void listChanged(String[] users);
	// error handlers
	public void reportError(String s);
	public void pgpError(String s);
	public void loggedOut();
}
