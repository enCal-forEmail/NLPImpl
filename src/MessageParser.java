import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Pattern;

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

public class MessageParser {
    private static AnnotationPipeline pipeline;
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
        props.setProperty("sutime.includeRange", "true");
        pipeline.addAnnotator(new TimeAnnotator("sutime", props));
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
		String body =  "Next spring, they met every Tuesday afternoon, from 1:00 pm to 3:00 pm. Three interesting dates are 18 Feb 1997 9am. july 20 3pm-4pm. our last meeting is 4 days from today TBD";
    	String subject = "Engr Club Meetings in Huang 23";
    	String timestamp = "faketimestamp";
    	ArrayList<String[]> eventsResult = getEventsInMessage(body, subject, timestamp);
	}

	// given timestamp and message id
	public static ArrayList<String[]> getEventsInMessage(String body, String subject, String timestamp) {

    
    	//String body = "Next spring, they met every Tuesday afternoon, from 1:00 pm to 3:00 pm.";
    	
    	Scanner scan = new Scanner(body);
    	scan.useDelimiter("\\n|;|\\.");
    	ArrayList<String[]> events = new ArrayList<String[]>();

    	while(scan.hasNext()){
    		String[] currentEvent = new String[3]; // date, start time, end time
    		String[] extras = new String[6]; // some capacity for extra things
    		int currExtraInd = 0;
    		int currDateInd = 1;
    		String text = scan.next();
    		Annotation annotation = new Annotation(text);
    		annotation.set(CoreAnnotations.DocDateAnnotation.class, SUTime.getCurrentTime().toString());
    		pipeline.annotate(annotation);
	    
    		//System.out.println(annotation.get(CoreAnnotations.TextAnnotation.class));
	    
    		List<CoreMap> timexAnnsAll = annotation.get(TimeAnnotations.TimexAnnotations.class);
    		for (CoreMap cm : timexAnnsAll) {
    			List<CoreLabel> tokens = cm.get(CoreAnnotations.TokensAnnotation.class);
    			//System.out.println(cm);
    			SUTime.Temporal temp = cm.get(TimeExpression.Annotation.class).getTemporal();
    			System.out.println(cm + " [from char offset " +
    					tokens.get(0).get(CoreAnnotations.CharacterOffsetBeginAnnotation.class) +
    					" to " + tokens.get(tokens.size() - 1).get(CoreAnnotations.CharacterOffsetEndAnnotation.class) + ']' +
    					" --> " + temp + " " + temp.getTimexType()); 
    			boolean doesMatch = Pattern.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}", temp.toString());
    			if(temp.getTimexType() == SUTime.TimexType.TIME){ // not a date, but a time
    				if(currDateInd < 3 && currentEvent[currDateInd] == null){
    					currentEvent[currDateInd++] = cm.toString();
    				} else if (currExtraInd < extras.length) {
    					extras[currExtraInd++] = cm.toString();
    				}
    			}
    			// a date that isn't a season
    			//else if (temp.getStandardTemporalType() != SUTime.StandardTemporalType.QUARTER_OF_YEAR && temp.getStandardTemporalType() != SUTime.StandardTemporalType.HALF_OF_YEAR && temp.getStandardTemporalType() != SUTime.StandardTemporalType.PART_OF_YEAR && temp.getStandardTemporalType() != SUTime.StandardTemporalType.SEASON_OF_YEAR && temp.getStandardTemporalType() != SUTime.StandardTemporalType.WEEK_OF_YEAR){ 
    			else if(!Pattern.matches("[0-9]{4}-[A-Z]{2}", temp.toString())){
    				if(doesMatch){
    					if(currentEvent[0] == null){
    						currentEvent[0] = temp.toString();
    					} else if (currExtraInd < extras.length){
    						extras[currExtraInd++] = temp.toString();
    					}
    					//continue;
    				}
    				else {
    					if(currentEvent[0] == null){
    						currentEvent[0] = cm.toString();
    					} else if (currExtraInd < extras.length){
    						extras[currExtraInd++] = cm.toString();
    					}
    				}
    			}
    			System.out.println("--");
    		}
    		events.add(currentEvent);
    		events.add(extras);
    	}
		for(String[] x : events){
			for(String y : x){
				System.out.print(y + ";;;");
			}
		System.out.println();
		}
		return events;
	}
}