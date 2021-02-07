import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Martín Gascón 764429
 * @author Eduardo Ruiz  764539
 * This class detects different types of date patterns
 */
public class DateDetector {

    public String PUBLISHED_RANGE_PATTERN = "(publicados|realizados) entre (el año|los años) (\\d\\d\\d\\d) y (\\d\\d\\d\\d)";
    public final String LAST_N_YEARS_PATTERN = "(publicados|realizados) en los últimos \\d+ años";

    public DateDetector() {
    }

    /**
     * This method detect if there is a date range in the spanish sentence like "published between [year] and [year] or
     * realized between [year] and [year] or realized in the last n [year] or published in the last n [year]
     * @param sentence Text to detect patterns.
     * @return A date range composed by a init year and a end year.
     */
    public Range getRangePattern(String sentence){
        Pattern rangePattern=Pattern.compile(PUBLISHED_RANGE_PATTERN);
        Matcher matcher=rangePattern.matcher(sentence);
       if(matcher.find()){
            matcher.group(1);
           return new Range(Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(4)));
        }

       Pattern lastYearsPattern=Pattern.compile(LAST_N_YEARS_PATTERN);
       Matcher lastYearsMatcher=lastYearsPattern.matcher(sentence);

       if(lastYearsMatcher.find()) {
           int offset = Integer.parseInt(matcher.group(1));
           int currentYear = Calendar.getInstance().get(Calendar.YEAR);
           return  new Range(currentYear - offset, currentYear);
       }

        return null;
    }

    /**
     * Class which define a year range.
     */
    public class Range{
        int beginYear;
        int endYear;

        public Range(int initYear, int endYear) {
            this.beginYear = initYear;
            this.endYear = endYear;
        }

        public int getBeginYear() {
            return this.beginYear;
        }

        public int getEndYear() {
            return this.endYear;
        }
    }
}
