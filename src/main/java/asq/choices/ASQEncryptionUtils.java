package asq.choices;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.Date;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

public class ASQEncryptionUtils {
    /**
     * Write out the contents of the provided file as a literal data packet in partial packet
     * format.
     *
     * @param out      the stream to write the literal data to.
     * @param fileType the {@link PGPLiteralData} type to use for the file data.
     * @param file     the file to write the contents of.
     * @param buffer   buffer to be used to chunk the file into partial packets.
     * @throws IOException if an error occurs reading the file or writing to the output stream.
     * @see PGPLiteralDataGenerator#open(OutputStream, char, String, Date, byte[])
     */
    public static void writeFileToLiteralData(
        OutputStream out,
        char fileType,
        String fileName,
        InputStream clearDataInput,
        byte[] buffer)
        throws IOException
    {
        PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();
        OutputStream pOut = lData.open(out, fileType, fileName, new Date(), buffer);
        pipeFileContents(clearDataInput, pOut, buffer.length);
    }

    private static void pipeFileContents(InputStream in, OutputStream pOut, int bufferSize)
        throws IOException
    {
        byte[] buf = new byte[bufferSize];

        try
        {
            int len;
            while ((len = in.read(buf)) > 0)
            {
                pOut.write(buf, 0, len);
            }

            pOut.close();
        }
        finally
        {
            Arrays.fill(buf, (byte)0);
            try
            {
                in.close();
            }
            catch (IOException ignored)
            {
                // ignore...
            }
        }
    }

    /**
     * Search a secret key ring collection for a secret key corresponding to keyID if it
     * exists.
     *
     * @param pgpSec a secret key ring collection.
     * @param keyID keyID we want.
     * @param pass passphrase to decrypt secret key with.
     * @return the private key.
     * @throws PGPException
     * @throws NoSuchProviderException
     */
    static PGPPrivateKey findSecretKey(PGPSecretKeyRingCollection pgpSec, long keyID, char[] pass)
        throws PGPException, NoSuchProviderException
    {
        PGPSecretKey pgpSecKey = pgpSec.getSecretKey(keyID);

        if (pgpSecKey == null)
        {
            return null;
        }

        return pgpSecKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(pass));
    }

}
