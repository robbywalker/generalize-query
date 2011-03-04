package greplin.extract;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.StringTokenizer;

interface ITokenMatcher {
	public ArrayList<Integer> getMatches(ListIterator<String> tokens);
}

class SingleMatcher implements ITokenMatcher {
	public SingleMatcher(HashSet<String> known) {
		this.known = known;
	}
	public ArrayList<Integer> getMatches(ListIterator<String> tokens) {
		String token = tokens.next();
		if (known.contains(token)) {
			ArrayList<Integer> matches = new ArrayList<Integer>(1);
			matches.add(1);
			return matches;
		}
		return null;
	}
	private HashSet<String> known;
}

class MultipleMatcher implements ITokenMatcher {
	final static int MAX_TOKENS = 6;
	public MultipleMatcher(ArrayList<HashSet<String>> known) {
		this.known = known;
	}
	public ArrayList<Integer> getMatches(ListIterator<String> tokens) {
		ArrayList<Integer> matches = new ArrayList<Integer>(MAX_TOKENS);
		String toMatch = "";
		for (int l = 1; tokens.hasNext() && l <= MAX_TOKENS && l < known.size(); ++l) {
			if (toMatch == "") {
				toMatch = tokens.next();
			} else {
				toMatch = toMatch + " " + tokens.next();
			}
			HashSet<String> knownForLength = known.get(l);
			if (knownForLength != null && knownForLength.contains(toMatch)) {
				matches.add(l);
			}
		}
		return matches;
	}
	private ArrayList<HashSet<String>> known;
}

class PatternMatcher {
	public PatternMatcher(
			HashSet<String> first, HashSet<String> last,
			ArrayList<HashSet<String>> place) {
		matchers = new TreeMap<String, ITokenMatcher>();
		matchers.put("<first>", new SingleMatcher(first));		
		matchers.put("<last>", new SingleMatcher(last));
		matchers.put("<place>", new MultipleMatcher(place));
	}
	protected void extractPatternsHelper(ArrayList<String> tokens, int start, String suffix, boolean hasSpecial, ArrayList<String> patterns) {
		if (start == tokens.size()) {
			if (hasSpecial) {
				patterns.add(suffix);
			}
			return;
		}
		String bestMatchName = "";
		int bestMatchCount = 0;
		for (Map.Entry<String, ITokenMatcher> entry: matchers.entrySet()) {
			ArrayList<Integer> matches = entry.getValue().getMatches(tokens.listIterator(start));
			if (matches == null || matches.isEmpty()) {
				continue;
			}
			int matchCount = matches.get(matches.size() - 1);
			if (matchCount > bestMatchCount) {
				bestMatchCount = matchCount;
				bestMatchName = entry.getKey();
			}
		}
		if (bestMatchCount > 0) {
			extractPatternsHelper(tokens, start + bestMatchCount, suffix.isEmpty() ? bestMatchName : suffix + ' ' + bestMatchName, true, patterns);
		} else {
			suffix = suffix.isEmpty() ? tokens.get(start) : suffix + ' ' + tokens.get(start);
			extractPatternsHelper(tokens, start + 1, suffix, hasSpecial, patterns);
		}
	}
	
	public void extractPatterns(ArrayList<String> tokens, ArrayList<String> patterns) {
		extractPatternsHelper(tokens, 0, "", false, patterns);
	}

	private TreeMap<String, ITokenMatcher> matchers;
}

public class PatternExtractorMain {
	final static String folder = "/Users/greplinguest/generalize-query/data/";
	final static String firstFile = folder + "firstnames.csv";
	final static String lastFile = folder + "lastnames.csv";
	final static String placeFile = folder + "places.txt";
	final static String dataFile = folder + "queries.txt";

	private static HashSet<String> parseNames(BufferedReader reader) throws IOException {
		reader.readLine();  // skip header line
		HashSet<String> names = new HashSet<String>();
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.isEmpty()) {
				continue;
			}
			names.add(line);
		}
		return names;
	}
	
	private static ArrayList<HashSet<String>> parsePlaces(BufferedReader reader) throws IOException {
		ArrayList<HashSet<String>> places = new ArrayList<HashSet<String>>();
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.isEmpty()) {
				continue;
			}
			StringTokenizer st = new StringTokenizer(line);
			String clean = "";
			int count = 0;
			while(st.hasMoreTokens()) {
				if (clean.isEmpty()) {
					clean = st.nextToken();
				} else {
					clean = clean + " " + st.nextToken();
				}
				++count;
			}
			if (count == 0) {
				continue;
			}
			HashSet<String> hashSet;
			if (count >= places.size() || (hashSet = places.get(count)) == null) {
				while (places.size() <= count) {
					places.add(null);
				}
				hashSet = new HashSet<String>();
				places.set(count, hashSet);
			}
			hashSet.add(clean);
		}
		return places;
	}
	private static class PatternCount implements Comparable<PatternCount> {
		public String pattern;
		public int count;
		public int compareTo(PatternCount o) {
			if (count < o.count) {
				return 1;
			} else if (count > o.count) {
				return -1;
			}
			return pattern.compareTo(o.pattern);
		}
	}
	private static void processQueries(PatternMatcher matcher, BufferedReader reader) throws IOException {
		HashMap<String, Integer> histogram = new HashMap<String, Integer>();
		String line;
		ArrayList<String> patterns = new ArrayList<String>();
		while ((line = reader.readLine()) != null) {
			ArrayList<String> tokens = new ArrayList<String>();
			StringTokenizer st = new StringTokenizer(line);
			while (st.hasMoreTokens()) {
				tokens.add(st.nextToken());
			}
			patterns.clear();
			matcher.extractPatterns(tokens, patterns);
			for (Iterator<String> it = patterns.iterator(); it.hasNext();) {
				String pattern = it.next();
				Integer count = histogram.get(pattern);
				if (count == null) {
					count = 1;
				} else {
					count += 1;
				}
				histogram.put(pattern, count);
			}
		}
		ArrayList<PatternCount> patternCounts = new ArrayList<PatternCount>(histogram.size());
		for (Map.Entry<String, Integer> entry: histogram.entrySet()) {
			PatternCount pc = new PatternCount();
			pc.pattern = entry.getKey();
			pc.count = entry.getValue();
			patternCounts.add(pc);
		}
		Collections.sort(patternCounts);
		for (int i = 0; i < 100; ++i) {
			if (patternCounts.get(i).count >= 2) {
				System.out.println(patternCounts.get(i).pattern + ' ' + patternCounts.get(i).count);
			}
		}
	}

	public static void main(String[] args) throws IOException {
		ArrayList<HashSet<String>> place = parsePlaces(new BufferedReader(new FileReader(placeFile)));
		HashSet<String> first = parseNames(new BufferedReader(new FileReader(firstFile)));
		HashSet<String> last = parseNames(new BufferedReader(new FileReader(lastFile)));
		PatternMatcher matcher = new PatternMatcher(first, last, place);

		processQueries(matcher, new BufferedReader(new FileReader(dataFile)));
	}
}
