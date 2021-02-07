import java.util.HashMap;
import java.util.Map;

/**
 * @author Martín Gascón 764429
 * @author Eduardo Ruiz  764539
 * Abstract class which restrict to the child detector classes
 */
public abstract class Detector {

    /**
     * Process a string deleting the accents and punctuation signs.
     * @param s string to normalize
     * @return a normalized string without accents and punctuation signs.
     */
    public static String normalize(String s) {
        s = s.replaceAll("\\p{P}","");
        s = s.replace("á", "a");
        s = s.replace("é", "e");
        s = s.replace("í", "i");
        s = s.replace("ó", "o");
        s = s.replace("ú", "u");
        s = s.replace("\n", "");
        s = s.replace("\t", "");
        return s;
    }

    /**
     * Detect a pattern into a string sentence
     * @param sentence string variable to detect a pattern
     * @return true if match the pattern with the sentence.
     */
    public abstract boolean detect(String sentence);

    /**
     * Load de data from files and initialize de pattern dataset.
     * @return return true if there is no problem in the file read.
     */
    public abstract boolean loadData();
}
