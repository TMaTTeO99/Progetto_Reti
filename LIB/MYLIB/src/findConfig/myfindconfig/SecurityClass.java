import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Random;

//classe usata per la produzione della chiave e per la cifratura e decifrazione dei dati
public class SecurityClass {

    private static int secret = -1;//segreto degli interlocutori nel protocollo DH


    public static int getSecret() throws NullPointerException {

        //se non viene richiamato prima Compute_C sollevo un exception
        if(secret == -1) throw new NullPointerException();

        return secret;
    }
    public static long Compute_C(int g, int p) {

        //calcolo il segreto
        Random rnd = new Random();
        secret = 0;
        secret = rnd.nextInt((p-1) - 2) + 2;
        return powInModulo(g, secret, p);

    }
    //metodo usato per calcolare l esponenziale in modulo
    public static long powInModulo(long b, long exp, long mod) {

        long result = 1;
        b = b % mod; // Assicurati che il valore di base sia compreso tra 0 e (modulo - 1)

        while (exp > 0) {
            if (exp % 2 == 1) {
                result = (result * b) % mod;
            }
            b = (b * b) % mod;
            exp /= 2;
        }
        return result;
    }
    //moetodi di cifratura e decifrazione
    public static byte [] encrypt(String message, String key) {

        byte [] dati = null;
        try {
            Cipher cphr = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec sKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            cphr.init(Cipher.ENCRYPT_MODE, sKey);
            dati = cphr.doFinal(message.getBytes(StandardCharsets.UTF_8));
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
        return dati;
    }
    public static String decrypt(byte [] message, String key) {

        String dati = null;
        try {
            Cipher cphr = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec sKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            cphr.init(Cipher.DECRYPT_MODE, sKey);
            dati = new String(cphr.doFinal(message), StandardCharsets.UTF_8);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return dati;
    }


}
