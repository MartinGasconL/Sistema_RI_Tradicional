import java.io.*;
import java.util.HashMap;

/**
 * @author Martín Gascón 764429
 * @author Eduardo Ruiz  764539
 * This class offers different types of location detection into a text.
 */
public class SpanishLocationsDetector extends Detector{
    private final String COUNTRIES_FILE_PATH = "spanish-location-names/countries.txt";
    private final String REGION_FILE_PATH = "spanish-location-names/regions.txt";
    private HashMap<String, Boolean> countries;
    private HashMap<String, Boolean> regions;

    public SpanishLocationsDetector() {
        loadData();
    }

    /**
     * Load data from a file into a map.
     * @param path Path of the data file.
     * @return Return a Map with de data loaded.
     * @throws IOException
     */
    private HashMap<String, Boolean> load(String path) throws IOException {
        HashMap<String, Boolean> retval = new HashMap<>();

        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
        String line;

        while ((line = in.readLine()) != null){
            retval.put(line,true);
        }
        in.close();

        return retval;
    }

    @Override
    public boolean detect(String token) {
        return countries.get(normalize(token).toUpperCase()) != null ||
                regions.get(normalize(token).toUpperCase()) != null ;
    }

    @Override
    public boolean loadData() {
        try{
            this.countries = load(COUNTRIES_FILE_PATH);
            this.regions = load(REGION_FILE_PATH);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
