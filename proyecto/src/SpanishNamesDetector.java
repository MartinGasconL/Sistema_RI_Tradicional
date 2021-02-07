import java.io.*;
import java.util.HashMap;

/**
 * @author Martín Gascón 764429
 * @author Eduardo Ruiz  764539
 * This class offers different types of name detections.
 */
public class SpanishNamesDetector extends Detector{
    private final String NAMES_FILE_PATH = "spanish-names-master/names.txt";
    private final String SURNAMES_FILE_PATH = "spanish-names-master/surnames.txt";
    HashMap<String, Boolean> names;
    HashMap<String, Boolean> surnames;

    public SpanishNamesDetector() {
       loadData();
    }

    /**
     * This method load the file which contains the surname data into a hashmap.
     * @return Hashmap with the surname data loaded
     * @throws IOException
     */
    private HashMap<String, Boolean> loadSurnames() throws IOException {
        HashMap<String, Boolean> retval = new HashMap<>();

        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(SURNAMES_FILE_PATH)));
        String line;

        while ((line = in.readLine()) != null){
            retval.put(line,true);
        }
        in.close();

        return retval;
    }

    /**
     * This method load the file which contains the name data into a hashmap.
     * @return Hashmap with the name data loaded
     *
     */
    private HashMap<String, Boolean> loadNames() throws IOException {
        HashMap<String, Boolean> retval = new HashMap<>();
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(NAMES_FILE_PATH)));
        String line;

        while ((line = in.readLine()) != null){
            retval.put(line,true);
        }
        in.close();

        return retval;
    }

    @Override
    public boolean detect(String token) {
        return names.get(normalize(token).toUpperCase()) != null || surnames.get(normalize(token).toUpperCase()) != null;
    }

    @Override
    public boolean loadData() {
        try {
            names = loadNames();
            surnames = loadSurnames();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
