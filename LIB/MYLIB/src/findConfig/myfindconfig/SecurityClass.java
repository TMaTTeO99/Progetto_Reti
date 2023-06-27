import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Random;

//classe usata per la produzione della chiave e per la cifratura e decifrazione dei dati
public class SecurityClass {

    private static BigInteger secret = new BigInteger("-1");//segreto degli interlocutori nel protocollo DH


    public static BigInteger getSecret() throws NullPointerException {

        //se non viene richiamato prima Compute_C sollevo un exception
        if(secret.compareTo(new BigInteger("-1")) == 0) throw new NullPointerException();

        return secret;
    }
    public static BigInteger Compute_C(int g, int p) {

        BigInteger P = new BigInteger(String.valueOf(p));
        BigInteger G = new BigInteger(String.valueOf(g));

        //il valore x che devo produrre deve essere : 1 < x < p -1
        BigInteger lowbound = new BigInteger("1");
        BigInteger highbound = new BigInteger(String.valueOf(p-1));

        SecureRandom random = new SecureRandom();//oggetto che mi permette di generare un numero random

        do {secret = new BigInteger(highbound.bitLength(), random);}
        while (secret.compareTo(lowbound) < 0 || secret.compareTo(highbound) > 0);

        return G.modPow(secret, P);

    }
    //metodo usato per la cifratura dei dati
    public static byte [] encrypt(String message, String key) {

        byte [] dati = null;
        try {
            Cipher cphr = Cipher.getInstance("AES/ECB/PKCS5Padding");//recupero l istanza dell AES
            SecretKeySpec sKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            cphr.init(Cipher.ENCRYPT_MODE, sKey);//setto il cifrario in modalita cifratura
            dati = cphr.doFinal(message.getBytes(StandardCharsets.UTF_8));//cifro i dati
        }
        catch (Exception e){return null;}
        return dati;
    }
    //metodo usato per la decifrazione dei dati
    public static String decrypt(byte [] message, String key) {

        String dati = null;
        try {
            Cipher cphr = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec sKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            cphr.init(Cipher.DECRYPT_MODE, sKey);//setto il cifrario in modalita decifrazione
            dati = new String(cphr.doFinal(message), StandardCharsets.UTF_8);
        }
        catch (Exception e) {return null;}
        return dati;
    }


}
