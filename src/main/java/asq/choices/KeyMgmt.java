package asq.choices;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;
import java.util.Iterator;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.bouncycastle.util.io.Streams;

public class KeyMgmt {
	private String path;
	private String secKeyFile;
	private String pubKeyFile;
	private PGPPublicKey pubKey;
	private PGPSecretKey secKey;
	private char[] passPhrase;

	public KeyMgmt(String path, String identity, char[] passPhrase) throws Exception {
		this.path = path;
		Path secKeyPath = Paths.get(path, identity + "_sekey.asc");
		secKeyFile = secKeyPath.toString();
		Path pubKeyPath = Paths.get(path, identity + "_pubkey.asc");
		pubKeyFile = pubKeyPath.toString();
		this.passPhrase = passPhrase;

		if (!checkKeys()) {
			createKeyPair(identity, passPhrase);
		}
		System.out.println("Using secret key from " + secKeyFile);
	}

	public KeyMgmt(String identity, char[] passPhrase) throws Exception {
		this(Paths.get(System.getProperty("user.dir")).toString(), identity, passPhrase);
	}

	public String getPubKeyFile() {
		return pubKeyFile;
	}

	public String getSecretKeyFile() {
		return secKeyFile;
	}

	public char[] getPassphrase() {
		return passPhrase;
	}

	public boolean checkKeys() throws Exception {
		File f = new File(secKeyFile);
		if (!f.exists()) {
			return false;
		}
		pubKey = readPublicKey(pubKeyFile);
		if (pubKey == null) {
			return false;
		}
		System.out.println("Public key ok");;
		secKey = readSecretKey(secKeyFile);
		if (secKey == null) {
			return false;
		}
		System.out.println("Secret key ok");;
		return true;
	}

	PGPPublicKey readPublicKey(String fileName) throws IOException, PGPException
    {
		InputStream keyIn = new FileInputStream(fileName);
        PGPPublicKey pubKey = readPublicKey(keyIn);
        keyIn.close();
        return pubKey;
    }

    /**
     * A simple routine that opens a key ring file and loads the first available key
     * suitable for encryption.
     *
     * @param input data stream containing the public key data
     * @return the first public key found.
     * @throws IOException
     * @throws PGPException
     *
     * This function is borrowed from the bouncycastle examples.
     */
    public PGPPublicKey readPublicKey(InputStream input) throws IOException, PGPException
    {
        PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(
            PGPUtil.getDecoderStream(input), new JcaKeyFingerprintCalculator());

        //
        // we just loop through the collection till we find a key suitable for encryption, in the real
        // world you would probably want to be a bit smarter about this.
        //

        Iterator keyRingIter = pgpPub.getKeyRings();
        while (keyRingIter.hasNext())
        {
            PGPPublicKeyRing keyRing = (PGPPublicKeyRing)keyRingIter.next();

            Iterator keyIter = keyRing.getPublicKeys();
            while (keyIter.hasNext())
            {
                PGPPublicKey key = (PGPPublicKey)keyIter.next();

                if (key.isEncryptionKey())
                {
                    return key;
                }
            }
        }

        throw new IllegalArgumentException("Can't find encryption key in key ring.");
    }

    PGPSecretKey readSecretKey(String fileName) throws IOException, PGPException
    {
        InputStream keyIn = new FileInputStream(fileName);
        PGPSecretKey secKey = readSecretKey(keyIn);
        keyIn.close();
        return secKey;
    }

    /**
     * A simple routine that opens a key ring file and loads the first available key
     * suitable for signature generation.
     *
     * @param input stream to read the secret key ring collection from.
     * @return a secret key.
     * @throws IOException on a problem with using the input stream.
     * @throws PGPException if there is an issue parsing the input stream.
     *
     *
     * This function is borrowed from the bouncycastle examples.
     */
    PGPSecretKey readSecretKey(InputStream input) throws IOException, PGPException
    {
        PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(
            PGPUtil.getDecoderStream(input), new JcaKeyFingerprintCalculator());

        //
        // we just loop through the collection till we find a key suitable for encryption, in the real
        // world you would probably want to be a bit smarter about this.
        //

        Iterator keyRingIter = pgpSec.getKeyRings();
        while (keyRingIter.hasNext())
        {
            PGPSecretKeyRing keyRing = (PGPSecretKeyRing)keyRingIter.next();

            Iterator keyIter = keyRing.getSecretKeys();
            while (keyIter.hasNext())
            {
                PGPSecretKey key = (PGPSecretKey)keyIter.next();

                if (key.isSigningKey())
                {
                    return key;
                }
            }
        }

        throw new IllegalArgumentException("Can't find signing key in key ring.");
    }






	public void createKeyPair(String identity, char[] passPhrase) throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
		kpg.initialize(1024);
		KeyPair kp = kpg.generateKeyPair();

		OutputStream secOut = new ArmoredOutputStream(new FileOutputStream(secKeyFile));
		OutputStream pubOut = new ArmoredOutputStream(new FileOutputStream(pubKeyFile));

        PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA1);
        PGPKeyPair          keyPair = new JcaPGPKeyPair(PGPPublicKey.RSA_GENERAL, kp, new Date());
        PGPSecretKey        secretKey = new PGPSecretKey(PGPSignature.DEFAULT_CERTIFICATION,
        									keyPair, identity, sha1Calc, null, null,
        									new JcaPGPContentSignerBuilder(keyPair.getPublicKey().getAlgorithm(),
        											HashAlgorithmTags.SHA1),
        									new JcePBESecretKeyEncryptorBuilder(PGPEncryptedData.CAST5, sha1Calc).setProvider("BC").build(passPhrase));

        System.out.println("Writing secret key to " + secKeyFile);
        secretKey.encode(secOut);
        secOut.close();

        System.out.println("Writing public key to " + pubKeyFile);
        PGPPublicKey key = secretKey.getPublicKey();
        key.encode(pubOut);
        pubOut.close();
	}








    static byte[] compressFile(String fileName, int algorithm) throws IOException
    {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(algorithm);
        PGPUtil.writeFileToLiteralData(comData.open(bOut), PGPLiteralData.BINARY,
            new File(fileName));
        comData.close();
        return bOut.toByteArray();
    }

    static byte[] compressFile(InputStream input, int algorithm) throws IOException
    {
    	byte[] buffer = new byte[32768];
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(algorithm);
        ASQEncryptionUtils.writeFileToLiteralData(comData.open(bOut), PGPLiteralData.BINARY,
            "pseudofilename", input, buffer);
        comData.close();
        return bOut.toByteArray();
    }


    /**
     *
     * @param out
     * @param clearInputData
     * @param encKey
     * @param armor
     * @param withIntegrityCheck
     * @throws IOException
     * @throws NoSuchProviderException
     *
     *
     * This function is borrowed from the bouncycastle examples and modified
     * according to the needs in this application.
     */
    public void encryptData(
    		OutputStream    out,
    		InputStream     clearInputData,
    		PGPPublicKey    encKey,
    		boolean         armor,
    		boolean         withIntegrityCheck)
    				throws IOException, NoSuchProviderException
    {
    	if (armor)
    	{
    		out = new ArmoredOutputStream(out);
    	}

    	try
    	{
    		byte[] bytes = compressFile(clearInputData, CompressionAlgorithmTags.ZIP);

    		PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(
    				new JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5).setWithIntegrityPacket(withIntegrityCheck).setSecureRandom(new SecureRandom()).setProvider("BC"));

    		encGen.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(encKey).setProvider("BC"));

    		OutputStream cOut = encGen.open(out, bytes.length);

    		cOut.write(bytes);
    		cOut.close();

    		if (armor)
    		{
    			out.close();
    		}
    	}
    	catch (PGPException e)
    	{
    		System.err.println(e);
    		if (e.getUnderlyingException() != null)
    		{
    			e.getUnderlyingException().printStackTrace();
    		}
    	}
    }






    /**
     *
     * @param in
     * @param keyIn
     * @param passwd
     * @param defaultFileName
     * @param out
     * @throws IOException
     * @throws NoSuchProviderException
     * @throws PGPException
     *
     *
     * This function is borrowed from the bouncycastle examples and modified
     * according to the needs in this application.
     */

    public void decryptData(
            InputStream in,
            InputStream keyIn,
            char[]      passwd,
            String      defaultFileName,
            OutputStream out)
            throws IOException, NoSuchProviderException, PGPException
        {
            in = PGPUtil.getDecoderStream(in);

                JcaPGPObjectFactory pgpF = new JcaPGPObjectFactory(in);
                PGPEncryptedDataList    enc;

                Object                  o = pgpF.nextObject();
                //
                // the first object might be a PGP marker packet.
                //
                if (o instanceof PGPEncryptedDataList)
                {
                    enc = (PGPEncryptedDataList)o;
                }
                else
                {
                    enc = (PGPEncryptedDataList)pgpF.nextObject();
                }

                //
                // find the secret key
                //
                Iterator                    it = enc.getEncryptedDataObjects();
                PGPPrivateKey               sKey = null;
                PGPPublicKeyEncryptedData   pbe = null;
                PGPSecretKeyRingCollection  pgpSec = new PGPSecretKeyRingCollection(
                    PGPUtil.getDecoderStream(keyIn), new JcaKeyFingerprintCalculator());

                while (sKey == null && it.hasNext())
                {
                    pbe = (PGPPublicKeyEncryptedData)it.next();

                    sKey = ASQEncryptionUtils.findSecretKey(pgpSec, pbe.getKeyID(), passwd);
                }

                if (sKey == null)
                {
                    throw new IllegalArgumentException("secret key for message not found.");
                }

                InputStream         clear = pbe.getDataStream(new JcePublicKeyDataDecryptorFactoryBuilder().setProvider("BC").build(sKey));

                JcaPGPObjectFactory    plainFact = new JcaPGPObjectFactory(clear);

                Object              message = plainFact.nextObject();

                if (message instanceof PGPCompressedData)
                {
                    PGPCompressedData   cData = (PGPCompressedData)message;
                    JcaPGPObjectFactory    pgpFact = new JcaPGPObjectFactory(cData.getDataStream());

                    message = pgpFact.nextObject();
                }

                if (message instanceof PGPLiteralData)
                {
                    PGPLiteralData ld = (PGPLiteralData)message;

                    String outFileName = ld.getFileName();
                    if (outFileName.length() == 0)
                    {
                        outFileName = defaultFileName;
                    }

                    InputStream unc = ld.getInputStream();
//                    OutputStream fOut = new BufferedOutputStream(new FileOutputStream(outFileName));
                    OutputStream fOut = out;

                    Streams.pipeAll(unc, fOut);

                    fOut.close();
                }
                else if (message instanceof PGPOnePassSignatureList)
                {
                    throw new PGPException("encrypted message contains a signed message - not literal data.");
                }
                else
                {
                    throw new PGPException("message is not a simple encrypted file - type unknown.");
                }

                if (pbe.isIntegrityProtected())
                {
                    if (!pbe.verify())
                    {
                        System.out.println("message failed integrity check");
                    }
                    else
                    {
                        System.out.println("message integrity check passed");
                    }
                }
                else
                {
                    System.out.println("no message integrity check");
                }
        }

    public void tests() throws NoSuchProviderException, IOException, PGPException {
    	String s = "hallo";

    	ByteArrayOutputStream bout = new ByteArrayOutputStream();

    	ByteArrayInputStream bin = new ByteArrayInputStream(s.getBytes());

    	encryptData(bout, bin, this.pubKey, true, true);

    	String enc = bout.toString();

    	System.out.println("Encrypted:\n" + enc);

    	bout = new ByteArrayOutputStream();
    	decryptData(new ByteArrayInputStream( enc.getBytes() ),
    			new FileInputStream(secKeyFile),
    			"password".toCharArray(),
    			"nuet",
    			bout);

    	String dec = bout.toString();
    	System.out.println("Decrypted:\n" + dec);
    }
    public static void main(String[] args) throws Exception {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

    	KeyMgmt k = new KeyMgmt("user01", "password".toCharArray());

    	System.out.println("This is the key:" + k.pubKey.toString());
    	k.tests();
    }

}
