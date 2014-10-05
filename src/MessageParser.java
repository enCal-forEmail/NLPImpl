import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.AnnotationPipeline;
import edu.stanford.nlp.pipeline.PTBTokenizerAnnotator;
import edu.stanford.nlp.pipeline.WordsToSentencesAnnotator;
import edu.stanford.nlp.time.SUTime;
import edu.stanford.nlp.time.TimeAnnotations;
import edu.stanford.nlp.time.TimeAnnotator;
import edu.stanford.nlp.time.TimeExpression;
import edu.stanford.nlp.util.CoreMap;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Partial;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;

public class MessageParser {
    public static final Comparator<Partial> dateComparator = new Comparator<Partial>() {
        @Override
        public int compare(Partial partial, Partial partial2) {
            return partial.toString("y-M-d").compareTo(partial2.toString("y-M-d"));
        }
    };
    public static final Comparator<Partial> timeComparator = new Comparator<Partial>() {
        @Override
        public int compare(Partial partial, Partial partial2) {
            return partial.toString("H-m").compareTo(partial2.toString("H-m"));
        }
    };
    private static AnnotationPipeline pipeline;

    private static String[] allLocs = { "FIRST CHURCH OF CHRIST, SCIENTIST", "CHURCH OF THE GOOD SHEPHERD, EPISCOPAL", "WESTMINSTER PRESBYTERIAN CHURCH", "ST. JOHN'S PRESBYTERIAN CHURCH", "BERKELEY WOMEN'S CITY CLUB", "TOWN AND GOWN CLUB", "BERKELEY CITY HALL", "WILLIAM R. THORSEN HOUSE", "ROSE WALK", "OLD JEFFERSON ELEMENTARY SCHOOL", "EDWARD F. NIEHAUS HOUSE", "JOSEPH W. HARRIS HOUSE", "PARK CONGREGATIONAL CHURCH", "CAPTAIN CHARLES C. BOUDROW HOUSE", "ANDREW COWPER LAWSON HOUSE", "DRAWING BUILDING", "NORTH GATE HALL", "BERKELEY DAY NURSERY", "JOHN GALEN HOWARD HOUSE", "GOLDEN SHEAF BAKERY", "BORJA HOUSE", "BARKER BLOCK", "THE STUDIO BUILDING", "FOX COURT", "BONITA APARTMENTS", "BONITA HALL", "MANUEL SILVA HOUSE", "JEREMIAH T. BURKE HOUSE", "MORSE BLOCK", "TOVERII TUPPA", "JOSEPH CLAPP COTTAGE", "CHARLES W. HEYWOOD HOUSE", "COLLEGE WOMEN'S CLUB", "DELAWARE STREET HISTORIC DISTRICT", "MISS ELEANOR M. SMITH HOUSE & COTTAGE", "GARFIELD JUNIOR HIGH SCHOOL", "UNITED STATES POST OFFICE", "THE HILLSIDE CLUB", "MRS. EDMUND P. KING BUILDING", "GEORGE MORGAN BUILDING", "ALBERT E. MONTGOMERY HOUSE", "SISTERNA HISTORIC DISTRICT", "SODA WATER WORKS BUILDING", "UNIVERSITY OF CALIFORNIA PRESS BUILDING", "S. J. SILL & CO. GROCERY & HARDWARE STORE", "MARTHA E. SELL BUILDING", "ERNEST ALVA HERON BUILDING", "FREDERICK H. DAKIN WAREHOUSE", "EDGAR JENSEN HOUSE", "WEBB BLOCK", "STANDARD DIE & SPECIALTY COMPANY", "BERKELEY PIANO CLUB", "SQUIRES BLOCK", "CLAREMONT COURT GATE AND STREET MARKERS", "WALLACE W. CLARK BUILDING", "ALFRED BARTLETT HOUSES", "OAKS THEATRE", "LAURA BELLE MARSH KLUEGEL HOUSE", "CALIFORNIA MEMORIAL STADIUM", "ELMWOOD HARDWARE BUILDING", "MARIE & FREDERICK A. HOFFMAN BUILDING", "LAWRENCE BERKELEY NATIONAL LABORATORY BEVATRON SITE", "PHI KAPPA PSI CHAPTER HOUSE", "ANNIE & FRED J. MARTIN HOUSE", "CLEPHANE BUILDING", "CHARLES A. WESTENBERG HOUSE", "WALLACE-SAUER HOUSE", "ENNOR'S RESTAURANT BUILDING", "BERNARD & ANNIE MAYBECK HOUSE NO. 1", "BERKELEY ICELAND", "FRED & AMY CORKILL HOUSE", "CAMBRIDGE APARTMENTS", "HEZLETT'S SILK STORE BUILDING", "BROWER HOUSES AND DAVID BROWER REDWOOD", "DONALD AND HELEN OLSEN HOUSE", "NEEDHAM-OBATA BUILDING", "MOBILIZED WOMEN OF BERKELEY BUILDING", "KOERBER BUILDING", "CAPITOL MARKET BUILDING", "UNIVERSITY YWCA", "FISH-CLARK HOUSE", "PELICAN BUILDING", "DUNCAN & JEAN MCDUFFIE HOUSE", "JOHN BOYD HOUSE", "UNIVERSITY ART MUSEUM", "MARY J. BERG HOUSE", "LUCINDA REAMES HOUSE NO. 1", "LUCINDA REAMES HOUSE NO. 2", "WILLIAM WILKINSON HOUSE"};

    private static TrieST<String> trie = new TrieST<String>();

    private static SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
    private static SimpleDateFormat dateParser2 = new SimpleDateFormat("yyyy-MM-dd'T'HH");

    private static Field origTemporalField;

    static {
        pipeline = new AnnotationPipeline();
        pipeline.addAnnotator(new PTBTokenizerAnnotator(false));
        pipeline.addAnnotator(new WordsToSentencesAnnotator(false));

        String modelDir = "models/";

        String sutimeRules = modelDir + "/sutime/defs.sutime.txt,"
                + modelDir + "/sutime/english.holidays.sutime.txt,"
                + modelDir + "/sutime/english.sutime.txt";
        Properties props = new Properties();
        props.setProperty("sutime.rules", sutimeRules);
        props.setProperty("sutime.binders", "0");
        props.setProperty("sutime.includeRange", "false");
        pipeline.addAnnotator(new TimeAnnotator("sutime", props));

        BufferedReader io = null;
        try {
            System.out.println("ADADASA");
            io = new BufferedReader(new FileReader("places.txt"));
            String line;
            while ((line = io.readLine()) != null) {
                System.out.println(line);
                trie.put(line, line);
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        try {
            origTemporalField = TimeExpression.class.getDeclaredField("origTemporal");
            origTemporalField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
    public static String getLocation(String body, String subject){ 
        ArrayList<String> locs = (ArrayList<String>) Arrays.asList(allLocs);
        for(String loc : locs){
            if (body.toUpperCase().contains(loc) || subject.toUpperCase().contains(loc)){
                return loc;
            }
        }
        
        return "Not Availble";
    }

	public static void main(String[] args){
//		String body = "Hey Rocky!  Love learning about politics? Enjoy trivia games? Are you super competitive? Do you just like having fun and eating food?  Come on out to the  Political Trivia Study Break  Join The American Whig-Cliosophic Society for this year's first study break on Thursday at 7:30pm in the Whig Senate Chamber! Come settle the age-old question! Who's smarter: Whig or Clio? The winning side will receive a prize!  Pizza and other foods will be served as well!";
//		String body = "Hey Rocky!  Love learning about politics? Enjoy trivia games? Are you super competitive? Do you just like having fun and eating food?  Come on out to the  Political Trivia Study Break  Join The American Whig-Cliosophic Society for this year's first study break at 7:30pm in the Whig Senate Chamber! Tomorrow at 7:30 pm! From 7:30pm to 10:12pm. Come settle the age-old question! Who's smarter: Whig or Clio? The winning side will receive a prize!  Pizza and other foods will be served as well!";

        String body = "Hey guys,\r\n\r\nYou should come to rock climbing tomorrow! We have enough drivers for more\r\npeople. There\'s no better way to get to know your fellow Blueprint members\r\nwhile developing your forearm muscles. If you plan on coming, just text or\r\nemail me to let me know!\r\n\r\nAlso, reminder for everyone that signed up, we are meeting at the channing\r\nside of Underhill parking lot at 2 PM!";

//        String body = "2PM! Celebrate the start of fall with the Princeton Student Events Committee's annual Fall Fest--don't miss out on pumpkin picking and decorating, great music, delicious food, and a fall photo booth!  TODAY:  Friday October 3 4-6pm Frist South Lawn (Rain location: Frist 100 level)";

        String subject = "[RockyWire] Fwd: TODAY: PSEC presents Fall Fest!!!";
    	String timestamp = "faketimestamp";
    	List<Event> eventsResult = getEventsInMessage(body, subject, timestamp);

        for (Event result : eventsResult) {
            System.out.println(result);
        }
	}

    public static String getEventLocation(String body) {
        String longestPrefix = "";
        for (int i = 0; i < body.length(); i++) {
            String newPrefix = trie.longestPrefixOf(body.substring(i));
            if (newPrefix.length() > longestPrefix.length()) {
                longestPrefix = newPrefix;
            }
        }
        return longestPrefix;
    }

    private static double getPeriodMinutes(SUTime.Temporal temp) {
        try {
            Period jodaTimePeriod = temp.getGranularity().getJodaTimePeriod();
            double mins = jodaTimePeriod.getYears();
            mins = mins * 365 + jodaTimePeriod.getDays();
            mins = mins * 24 + jodaTimePeriod.getHours();
            mins = mins * 60 + jodaTimePeriod.getMinutes();
            return mins;
        } catch (NullPointerException e) {
            return Double.POSITIVE_INFINITY;
        }
    }

    public static class Event {
        public DateTime start;
        public DateTime end;

        public Event(DateTime start, DateTime end) {
            this.start = start;
            this.end = end;
        }

        public Event(Partial date, Partial startTime, Partial endTime) {
            // TODO: time zone issues

            DateTimeFormatter df = DateTimeFormat.forPattern("y-M-d H-m");

            String dateString = date.toString("y-M-d");
            String startTimeString = startTime.toString("H-m");
            String endTimeString = endTime.toString("H-m");

            this.start = DateTime.parse(dateString + " " + startTimeString, df);
            this.end = DateTime.parse(dateString + " " + endTimeString, df);

            if (start.compareTo(end) > 0) {
                end = end.withDurationAdded(1000L * 60 * 60 * 24, 1);
            }
        }

        public String toString() {
            return start.toString() + " " + end.toString();
        }
    }

	// given timestamp and message id
	public static List<Event> getEventsInMessage(String body, String subject, String timestamp) {
        List<Partial> dates = new ArrayList<Partial>();
        List<Partial> times = new ArrayList<Partial>();

        Annotation annotation = new Annotation(body);
        annotation.set(CoreAnnotations.DocDateAnnotation.class, SUTime.getCurrentTime().toString());
        pipeline.annotate(annotation);

        List<CoreMap> timexAnnsAll = annotation.get(TimeAnnotations.TimexAnnotations.class);
        List<SUTime.PartialTime> temporalQueue = new ArrayList<SUTime.PartialTime>();

        for (CoreMap cm : timexAnnsAll) {
            System.out.println(cm);
            List<CoreLabel> tokens = cm.get(CoreAnnotations.TokensAnnotation.class);

            try {
                Object temp = origTemporalField.get(cm.get(TimeExpression.Annotation.class));

                if (SUTime.Time.class.isAssignableFrom(temp.getClass())) {
                    Partial jodaTimePartial = ((SUTime.Time) temp).getJodaTimePartial();
                    if (jodaTimePartial != null) {
                        DateTimeFieldType[] fields = jodaTimePartial.getFieldTypes();
                        if (Arrays.asList(fields).contains(DateTimeFieldType.dayOfMonth())) {
                            SUTime.Temporal temp2 = cm.get(TimeExpression.Annotation.class).getTemporal();
                            if (temp2.getClass() == SUTime.PartialTime.class) {
                                temporalQueue.add((SUTime.PartialTime) temp2);
                            }
                        } else {
                            temporalQueue.add(((SUTime.PartialTime) temp));
                        }
                    } else {
                        SUTime.Temporal temp2 = cm.get(TimeExpression.Annotation.class).getTemporal();
                        if (temp2.getClass() == SUTime.PartialTime.class) {
                            temporalQueue.add((SUTime.PartialTime) temp2);
                        }
                    }
                } else {
                    SUTime.Temporal temp2 = cm.get(TimeExpression.Annotation.class).getTemporal();
                    if (temp2.getClass() == SUTime.PartialTime.class) {
                        temporalQueue.add((SUTime.PartialTime) temp2);
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        for (SUTime.PartialTime temp : temporalQueue) {

//            System.out.println(cm + " [from char offset " +
//                    tokens.get(0).get(CoreAnnotations.CharacterOffsetBeginAnnotation.class) +
//                    " to " + tokens.get(tokens.size() - 1).get(CoreAnnotations.CharacterOffsetEndAnnotation.class) + ']' +
//                    " --> " + temp + " " + temp.getTimexType());

            Partial partial = temp.getJodaTimePartial();
            DateTimeFieldType[] fieldTypes = partial.getFieldTypes();
            List<DateTimeFieldType> fieldTypeList = Arrays.asList(fieldTypes);
            if (fieldTypeList.contains(DateTimeFieldType.dayOfMonth())) {
                dates.add(partial);
            }
            if (fieldTypeList.contains(DateTimeFieldType.hourOfDay())
                    && temp.getUncertaintyGranularity().getJodaTimePeriod().toStandardMinutes().getMinutes() <= 60) {
                if (!fieldTypeList.contains(DateTimeFieldType.minuteOfHour())) {
                    partial = partial.with(DateTimeFieldType.minuteOfHour(), 0);
                }
                times.add(partial);
            }
        }

        dates = removeDuplicates(dates, dateComparator);
        times = removeDuplicates(times, timeComparator);

        if (times.size() == 1) {
            times.add(times.get(0));
        }

        Collections.sort(dates, dateComparator);
        Collections.sort(times, timeComparator);


        List<Event> events = new ArrayList<Event>();

        for (Partial date : dates) {
            for (int i = 0; i < times.size(); i++) {
                for (int j = i + 1; j < times.size(); j++) {
                    events.add(new Event(date, times.get(i), times.get(j)));
                }
            }
        }

        return events;
	}

    private static <T> List<T> removeDuplicates(List<T> l, Comparator<T> comparator) {
        Set<T> s = new TreeSet<T>(comparator);
        s.addAll(l);
        return new ArrayList<T>(Arrays.asList((T[]) s.toArray()));
    }
}
