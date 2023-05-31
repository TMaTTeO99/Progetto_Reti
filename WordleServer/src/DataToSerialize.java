//Classe che contiene un generico tipo di dato usato per permettere la corretta serializzazione dei dati
public class DataToSerialize <T extends Object> {

    private char Flag;//Variabile di istanza per identificare quale tipo di dato viene estratto dalla coda
    private T Dato;//dato da usare, o da usare per recuperare i dati dalla ConcurrentHashMap Registarti o da serializzare direttamente
    public DataToSerialize(T d, char f) {
        Dato = d;
        Flag = f;
    }
    public T getDato() {return Dato;}
    public char getFlag() {return Flag;}
    public void setFlag(char f) {Flag = f;}
    public void setDato(T d) {Dato = d;}

}
