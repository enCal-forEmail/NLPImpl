import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

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
import org.joda.time.Period;

public class MessageParser {
    private static AnnotationPipeline pipeline;
    private static TrieST<String> trie = new TrieST<String>();

    private static SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
    private static SimpleDateFormat dateParser2 = new SimpleDateFormat("yyyy-MM-dd'T'HH");

    private static String[] allLocs = { "First Church of Christ, Scientist", "Church of the Good Shepherd, Episcopal", "Westminster Presbyterian Church", "St. John's Presbyterian Church", "Berkeley Women's City Club", "Town and Gown Club", "Berkeley City Hall", "William R. Thorsen House", "Rose Walk", "Old Jefferson Elementary School", "Edward F. Niehaus House", "Joseph W. Harris House", "Park Congregational Church", "Captain Charles C. Boudrow House", "Andrew Cowper Lawson House", "Drawing Building", "North Gate Hall", "Berkeley Day Nursery", "John Galen Howard House", "Golden Sheaf Bakery", "Borja House", "Barker Block", "The Studio Building", "Fox Court", "Bonita Apartments", "Bonita Hall", "Manuel Silva House", "Jeremiah T. Burke House", "Morse Block", "Toverii Tuppa", "Joseph Clapp Cottage", "Charles W. Heywood House", "College Women's Club", "Delaware Street Historic District", "Miss Eleanor M. Smith House & Cottage", "Garfield Junior High School", "United States Post Office", "The Hillside Club", "Mrs. Edmund P. King Building", "George Morgan Building", "Albert E. Montgomery House", "Sisterna Historic District", "Soda Water Works Building", "University of California Press Building", "S. J. Sill & Co. Grocery & Hardware Store", "Martha E. Sell Building", "Ernest Alva Heron Building", "Frederick H. Dakin Warehouse", "Edgar Jensen House", "Webb Block", "Standard Die & Specialty Company", "Berkeley Piano Club", "Squires Block", "Claremont Court Gate and Street Markers", "Wallace W. Clark Building", "Alfred Bartlett Houses", "Oaks Theatre", "Laura Belle Marsh Kluegel House", "California Memorial Stadium", "Elmwood Hardware Building", "Marie & Frederick A. Hoffman Building", "Lawrence Berkeley National Laboratory Bevatron Site", "Phi Kappa Psi Chapter House", "Annie & Fred J. Martin House", "Clephane Building", "Charles A. Westenberg House", "Wallace-Sauer House", "Ennor's Restaurant Building", "Bernard & Annie Maybeck House No. 1", "Berkeley Iceland", "Fred & Amy Corkill House", "Cambridge Apartments", "Hezlett's Silk Store Building", "Brower Houses and David Brower Redwood", "Donald and Helen Olsen House", "Needham-Obata Building", "Mobilized Women of Berkeley Building", "Koerber Building", "Capitol Market Building", "University YWCA", "Fish-Clark House", "Pelican Building", "Duncan & Jean McDuffie House", "John Boyd House", "University Art Museum", "Mary J. Berg House", "Lucinda Reames House No. 1", "Lucinda Reames House No. 2", "William Wilkinson House"};
    
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
    }
    public static String getLocation(String body, String subject){ 
        ArrayList<String> locs = (ArrayList<String>) Arrays.asList(allLocs);
        Scanner scan = new Scanner(body);
        while(scan.hasNext()){
            String curr = scan.next();
            if(locs.contains(curr)){
                scan.close();
                return curr;
            }
        }
        scan.close();
        return "Not Availble";
    }

	public static void main(String[] args){
//		String body = "Hey Rocky!  Love learning about politics? Enjoy trivia games? Are you super competitive? Do you just like having fun and eating food?  Come on out to the  Political Trivia Study Break  Join The American Whig-Cliosophic Society for this year's first study break on Thursday at 7:30pm in the Whig Senate Chamber! Come settle the age-old question! Who's smarter: Whig or Clio? The winning side will receive a prize!  Pizza and other foods will be served as well!";
//		String body = "Hey Rocky!  Love learning about politics? Enjoy trivia games? Are you super competitive? Do you just like having fun and eating food?  Come on out to the  Political Trivia Study Break  Join The American Whig-Cliosophic Society for this year's first study break at 7:30pm in the Whig Senate Chamber! Tomorrow at 7:30 pm! From 7:30pm to 10:12pm. Come settle the age-old question! Who's smarter: Whig or Clio? The winning side will receive a prize!  Pizza and other foods will be served as well!";

        String body = "You are invited to join the Princeton Quadrangle Club for our member's event, Jazz Club Night, this Saturday, October 4th from 9pm to midnight. Come and mingle with our members while enjoying a variety of beverages and a live jazz band. Semi-formal attire is enouraged. Sophomores will be admitted with PUID. Please RSVP here. ";

        String subject = "Engr Club Meetings in Huang 23";
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

        public Event(DateTime date, DateTime startTime, DateTime endTime) {
            // TODO: time zone issues

            this.start = date.withTime(startTime.getHourOfDay(), startTime.getMinuteOfHour(), 0, 0);
            this.end = date.withTime(endTime.getHourOfDay(), endTime.getMinuteOfHour(), 0, 0);

            if (start.compareTo(end) <= 0) {
                end = end.withDurationAdded(1000L * 60 * 60 * 24, 1);
            }
        }

        public String toString() {
            return start.toString() + " " + end.toString();
        }
    }

	// given timestamp and message id
	public static List<Event> getEventsInMessage(String body, String subject, String timestamp) {
        List<SUTime.Temporal> dates = new ArrayList<SUTime.Temporal>();
        List<SUTime.Temporal> times = new ArrayList<SUTime.Temporal>();

        List<SUTime.Temporal> temporals = new ArrayList<SUTime.Temporal>();

//        body = "Next spring, they met every Tuesday afternoon, from 1:00 pm to 3:00 pm.";

        Annotation annotation = new Annotation(body);
        annotation.set(CoreAnnotations.DocDateAnnotation.class, SUTime.getCurrentTime().toString());
        pipeline.annotate(annotation);

        //System.out.println(annotation.get(CoreAnnotations.TextAnnotation.class));

        List<CoreMap> timexAnnsAll = annotation.get(TimeAnnotations.TimexAnnotations.class);
        List<SUTime.Temporal> temporalQueue = new ArrayList<SUTime.Temporal>();

        for (CoreMap cm : timexAnnsAll) {
            System.out.println(cm);
            List<CoreLabel> tokens = cm.get(CoreAnnotations.TokensAnnotation.class);
            //System.out.println(cm);

//            if (cm.has(TimeExpression.ChildrenAnnotation.class)) {
//                for (Object item : cm.get(TimeExpression.ChildrenAnnotation.class)) {
//                    if (item.getClass() == Annotation.class) {
//                        SUTime.Temporal temporal = ((Annotation) item).get(TimeExpression.Annotation.class).getTemporal();
//                        temporalQueue.add(temporal);
//                    }
//                }
//            } else {
                temporalQueue.add(cm.get(TimeExpression.Annotation.class).getTemporal());
//            }
        }

        for (SUTime.Temporal temp : temporalQueue) {

//            System.out.println(cm + " [from char offset " +
//                    tokens.get(0).get(CoreAnnotations.CharacterOffsetBeginAnnotation.class) +
//                    " to " + tokens.get(tokens.size() - 1).get(CoreAnnotations.CharacterOffsetEndAnnotation.class) + ']' +
//                    " --> " + temp + " " + temp.getTimexType());

            if (temp.getTimexType() == SUTime.TimexType.TIME
                    && getPeriodMinutes(temp) < 100){ // not a date, but a time
                times.add(temp);
            } else if (temp.getTimexType() == SUTime.TimexType.DATE
                    && getPeriodMinutes(temp) < 1500) {
                dates.add(temp);
            }

            // a date that isn't a season
            //else if (temp.getStandardTemporalType() != SUTime.StandardTemporalType.QUARTER_OF_YEAR && temp.getStandardTemporalType() != SUTime.StandardTemporalType.HALF_OF_YEAR && temp.getStandardTemporalType() != SUTime.StandardTemporalType.PART_OF_YEAR && temp.getStandardTemporalType() != SUTime.StandardTemporalType.SEASON_OF_YEAR && temp.getStandardTemporalType() != SUTime.StandardTemporalType.WEEK_OF_YEAR){
//            else if(!Pattern.matches("[0-9]{4}-[A-Z]{2}", temp.toString())){
//                if(Pattern.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}", temp.toString())){
//                    if(currentEvent[0] == null){
//                        currentEvent[0] = temp.toString();
//                    } else if (currExtraInd < extras.length){
//                        extras[currExtraInd++] = temp.toString();
//                    }
//                    //continue;
//                }
//                else {
//                    if(currentEvent[0] == null){
//                        currentEvent[0] = cm.toString();
//                    } else if (currExtraInd < extras.length){
//                        extras[currExtraInd++] = cm.toString();
//                    }
//                }
//            }
        }

        Comparator<SUTime.Temporal> temporalComparator = new Comparator<SUTime.Temporal>() {
            @Override
            public int compare(SUTime.Temporal temporal, SUTime.Temporal temporal2) {
                return temporal.getTime().compareTo(temporal2.getTime());
            }
        };

        Collections.sort(dates, temporalComparator);
        Collections.sort(times, temporalComparator);

        if (dates.isEmpty()) {
            if (times.isEmpty()) {
                return null;
            } else {
                dates.add(times.get(0));
            }
        }
        if (times.size() == 1) {
            times.add(times.get(0));
        }

        List<DateTime> timesAsDateObjects = new ArrayList<DateTime>();
        for (SUTime.Temporal time : times) {
            timesAsDateObjects.add(toDate(time));
        }

        List<Event> events = new ArrayList<Event>();


        for (SUTime.Temporal date : dates) {
            DateTime dt = toDate(date);
            for (int i = 0; i < timesAsDateObjects.size(); i++) {
                for (int j = i + 1; j < timesAsDateObjects.size(); j++) {
                    events.add(new Event(dt, timesAsDateObjects.get(i), timesAsDateObjects.get(j)));
                }
            }
        }

        return events;
	}

    private static DateTime toDate(SUTime.Temporal time) {
        return time.getTime().getJodaTimeInstant().toDateTime();
    }
}