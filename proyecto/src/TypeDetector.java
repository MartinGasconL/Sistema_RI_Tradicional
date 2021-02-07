import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Martín Gascón 764429
 * @author Eduardo Ruiz  764539
 * This class detects the type of academic work of a sentence.
 */
public class TypeDetector{

    enum AcademicWorkType{
        TESIS ("TESIS"), TFG("TAZ-TFG"), TFM("TAZ-TFM");
        private String value;

        AcademicWorkType(String value){
            this.value = value;
        }
        public String getValue(){
            return value;
        }
    }
    static String tfgNames[] = {"tfg", "trabajo de fin de grado",
            "trabajos de fin de grado", "taz-tfg"};
    static String tfmNames[] = {"tfm", "trabajo de fin de master",
            "trabajos de fin de master", "taz-tfm"};

    /**
     * Detect the type of a academic work
     * @param sentence Sentence to detect the type pattern.
     * @return return a set with the types of academic works
     */
    static Set<AcademicWorkType> detectWorkType(String sentence){
        sentence = sentence.toLowerCase();
        Set<AcademicWorkType> retval = new HashSet<>();
        boolean tfg = false;
        boolean tfm = false;

        for(String term : tfgNames){
            if(sentence.contains(term)){
                retval.add(AcademicWorkType.TFG);
                tfg = true;
                break;
            }
        }

        for(String term : tfmNames){
            if(sentence.contains(term)){
                retval.add(AcademicWorkType.TFM);
                tfm = true;
                break;
            }
        }

        if(sentence.contains("trabajos") && !tfm && !tfg) {
            retval.add(AcademicWorkType.TFG);
            retval.add(AcademicWorkType.TFM);
        }

        if(sentence.contains("tesis"))
            retval.add(AcademicWorkType.TESIS);

        return retval;
    }
}
