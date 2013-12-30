package Parser;
import java.util.ArrayList; 
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.lang.String;
import java.util.HashMap; 


public class NameMatch {

	public boolean overrideMinLength = true; //whether we will override the minLength value to take into account maxdiff
	private boolean frontTruncation = false; // whether we expect the fuzzy match to be truncated	

	private HashMap<String,Boolean> ValidHashSet, FullValidHashSet;
	private HashMap<String, HashMap<String,Boolean>> ValidHashSetByVal;
	
	private double diffProportion = 0.25; // proportion by which match can be different
	private int maxDiff = 2;	// maximum score by which match can be different
	private int minLength = 3;  // miniumum length of string before namematch can occur
	
	public class Match {
		
		public String match;
		public int score;
		
		public Match(String b, int s){
			this.match=b;
			this.score=s;
		}
	
	}
	public void setMaxDiff(int m) {
		this.maxDiff = m;
	}
	public void setDiffProportion(double d) {
		this.diffProportion = d;
	}
	public void setMinLength(int m){
		this.minLength = m;
	}
	
	public int getMaxDiff() {
		return this.maxDiff;
	}
	public double getDiffProportion(){
		return this.diffProportion;
	}
	public int getMinLength(){
		return this.minLength;
	}	
	
	// returns best match for a string
	public Match BestMatch(String Orig) {
		
		Match bm = new Match("",999);
		int currentScore = bm.score;
		
		int minLen;
		if (overrideMinLength) minLen = Math.max(maxDiff + 1, minLength);
		else minLen = minLength;

		if (ValidHashSet == null) return bm; // no valid values, return null
		if (ValidHashSet.containsKey(Orig)) { // exact match
			bm.match = Orig;
			bm.score = 0;
			return bm;
		}
		if (Orig.replaceAll("[0-9]","").length() < minLen) return bm; // string is too short to spellcheck

	    Iterator it = ValidHashSet.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        String potentialMatch = (String) pairs.getKey();
			currentScore = score(Orig.toUpperCase(), potentialMatch.toUpperCase());
			if (currentScore > bm.score) continue; // no better than other matches
			bm.score = currentScore;
			bm.match = potentialMatch;	        
	    }

		if (bm.score > maxDiff || (double)bm.score/(double)Orig.length() > diffProportion) bm.match = "";
		//if (bm.match.length() > 0) StdOut.println(Orig + "-->" + bm.match + "(" + bm.score + ")");
		return bm;
	}

	// returns best match for a string within a group
	public Match BestMatch(String matchByVal, String Orig) {
		Match bm = new Match("",999);
		if (ValidHashSetByVal == null) throw new RuntimeException("Group variable not specified in namematch constructor");
		ValidHashSet = ValidHashSetByVal.get(matchByVal);
		bm = BestMatch(Orig);
		return bm;
	}

	// returns best match for a string within any of a list of groups
	public Match BestMatch(List<String> matchByVals, String Orig) {
		Match bm = new Match("",999);
		if (ValidHashSetByVal == null) throw new RuntimeException("Group variable not specified in namematch constructor");
		ValidHashSet = new HashMap<String, Boolean>();
		if (matchByVals.size()>0) for (String matchByVal : matchByVals) ValidHashSet.putAll(ValidHashSetByVal.get(matchByVal));
		else ValidHashSet = FullValidHashSet;
		bm = BestMatch(Orig);
		return bm;
	}	
	
	
	// constructor for linear set
	public NameMatch(DataLookup dl, String validVar) {
		this.ValidHashSet = (HashMap<String, Boolean>) dl.KeyLookup(validVar);
	}
	
	public NameMatch(DataLookup dl, String matchBy, String validVar) {
		this.FullValidHashSet = (HashMap<String, Boolean>) dl.KeyLookup(validVar);
		this.ValidHashSetByVal = dl.DupeKeyHashLookup(matchBy, validVar);
	}		

	// soundex algorithm
    public static String Soundex(String s) { 
    	if (s.length()==0) return "";
        char[] x = s.toUpperCase().toCharArray();
        char firstLetter = x[0];
        
        // convert letters to numeric code
        for (int i = 0; i < x.length; i++) {
            switch (x[i]) {
                case 'B':
                case 'F':
                case 'P':
                case 'V': { x[i] = '1'; break; }

                case 'C':
                case 'G':
                case 'J':
                case 'K':
                case 'Q':
                case 'S':
                case 'X':
                case 'Z': { x[i] = '2'; break; }

                case 'D':
                case 'T': { x[i] = '3'; break; }

                case 'L': { x[i] = '4'; break; }

                case 'M':
                case 'N': { x[i] = '5'; break; }

                case 'R': { x[i] = '6'; break; }

                default:  { x[i] = '0'; break; }
            }
        }

        // remove duplicates
        String output = "" + firstLetter;
        for (int i = 1; i < x.length; i++)
            if (x[i] != x[i-1] && x[i] != '0')
                output += x[i];

        // pad with 0's or truncate
        output = output + "0000";
        return output.substring(0, 5);
    }
    
    // levenshtein edit distance
    public static int complev(String s, String t){
        int m=s.length();
        int n=t.length();
        int[][]d=new int[m+1][n+1];
        for(int i=0;i<=m;i++){
          d[i][0]=i;
        }
        for(int j=0;j<=n;j++){
          d[0][j]=j;
        }
        for(int j=1;j<=n;j++){
          for(int i=1;i<=m;i++){
            if(s.charAt(i-1)==t.charAt(j-1)){
              d[i][j]=d[i-1][j-1];
            }
            else{
              d[i][j]=min((d[i-1][j]+1),(d[i][j-1]+1),(d[i-1][j-1]+1));
            }
          }
        }
        return(d[m][n]);
      }
    
    public static int min(int a,int b,int c){
    	return(Math.min(Math.min(a,b),c));
    }
	
	// spellcheck score
	// spellcheck score
	public static int score(String s, String t, boolean frontTruncation) {
		int distance = complev(s.replaceAll("[^A-Za-z0-9]+", "").toUpperCase(), t.replaceAll("[^A-Za-z0-9]+", "").toUpperCase());
		if(s.length() == 0 || t.length()==0 ) return distance;
		if (!frontTruncation) {
			if (Soundex(s).equals(Soundex(t))) distance--;
			if (!s.substring(0,1).toUpperCase().equals(t.substring(0,1).toUpperCase())) distance++;
		}
		return distance;
	}
	
	//spellcheck score
	public int score(String s, String t) {
		int distance = score(s,t, frontTruncation);
		return distance;
	}	


	public static void main(String[] args) {

		Match m;
		
		// score individual case
		int diff= score("GERRISON","GERRITSEN", false);
		StdOut.println(diff);
		
		// match a city name
		DataLookup cities = new DataLookup("/Parser/ny_cities.csv",",");
		NameMatch cityNameMatch = new NameMatch(cities, "name");
		cityNameMatch.maxDiff = 1;
		m = cityNameMatch.BestMatch("HASTINGS ON HUDSON");
		StdOut.println(m.match);
		
		// match a street name by boro
		DataLookup streets = new DataLookup("/Parser/edit_streetnames.csv",",");
		NameMatch streetNameMatch = new NameMatch(streets, "boronum_g", "streetname_g");
		m = streetNameMatch.BestMatch("3", "gerrison");
		StdOut.println(m.match);
	}

}
