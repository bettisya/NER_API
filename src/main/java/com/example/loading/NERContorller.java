package com.example.loading;
import banner.eval.BANNER;
import banner.types.Mention;
import banner.types.Sentence;
import banner.util.RankedList;
import dnorm.core.SynonymTrainer;
import dnorm.util.AbbreviationResolver;
import ncbi.PubtatorReader.Abstract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import edu.stanford.nlp.util.Triple;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by aning on 16-5-30.
 */

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping(value = "/", method = RequestMethod.GET)
public class NERContorller {

    static List<TmChemResult> results_Chemical;
    static List<DNormResult> results_Disease;
    static List<GeneralResult> results_General;

    @Autowired
    @Qualifier("mysqlJdbcTemplate")
    private JdbcTemplate mysqlTemplate;

    @CrossOrigin
    @RequestMapping(value = "submit", method = RequestMethod.POST)
    public Final_Results submit(@RequestBody RawText rawtext, HttpServletRequest request) throws IOException {

        String Type_ = "Entity Discovery API";
        String subType = rawtext.getSubType();
        if (subType == null)
            subType = "All";
        String text = rawtext.getText();

        String userIpAddress = request.getRemoteAddr(); // get the IP from user
//        System.out.println(userIpAddress);

        Timestamp timestamp = new Timestamp(System.currentTimeMillis()); //get current time
//        System.out.println(timestamp.toString());

//        String que = "select * from `apiCount`";
//        List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
//        maps = mysqlTemplate.queryForList(que);
//        System.out.println(maps.toString());

        mysqlTemplate.execute(String.format(
                "insert into `apiCount`( ip, type, subtype, time) values( '%s', '%s', '%s', '%s')",
                userIpAddress, Type_, subType, timestamp
        )); // write the record back to SQL


        if (!text.isEmpty()) {
            try {
                Abstract a = new Abstract();
                a.setId("");
                a.setTitleText("");
                a.setAbstractText(text);
                results_Disease = null;
                results_General = null;
                results_Chemical = null;

                if (subType.equals("Chemical") ) {
                    results_Chemical = process_Chemical(a);
                    Collections.sort(results_Chemical, new Comparator<TmChemResult>() {
                        @Override
                        public int compare(TmChemResult o1, TmChemResult o2) {
                            return o1.getStartChar() - o2.getStartChar();
                        }
                    });
                }
                else if (subType.equals("Disease") ) {
                    results_Disease = process_Disease(a);
                    Collections.sort(results_Disease, new Comparator<DNormResult>() {
                        @Override
                        public int compare(DNormResult r1, DNormResult r2) {
                            return r1.getStartChar() - r2.getStartChar();
                        }
                    });
                }
                else if (subType.equals("General") ) {
                    results_General = process_General(text);
                }
                else if (subType.equals("All")) {
                    results_Chemical = process_Chemical(a);
                    Collections.sort(results_Chemical, new Comparator<TmChemResult>() {
                        @Override
                        public int compare(TmChemResult o1, TmChemResult o2) {
                            return o1.getStartChar() - o2.getStartChar();
                        }
                    });

                    results_Disease = process_Disease(a);
                    Collections.sort(results_Disease, new Comparator<DNormResult>() {
                        @Override
                        public int compare(DNormResult r1, DNormResult r2) {
                            return r1.getStartChar() - r2.getStartChar();
                        }
                    });

                    results_General = process_General(text);
                }

                Final_Results end = new Final_Results(results_Chemical, results_Disease, results_General);
                return end;

            } catch (Exception e) {
                return null;
            }
        } else {
            return null;
        }
    }

    public static class RawText {
        private String text;
        private String subType;
//        private String Type;
//        private String getType() {return Type;}
        public String getSubType() { return  subType;}
        public String getText() {
            return text;
        }
    }

    private static List<GeneralResult> process_General(String text) throws Exception {
//        String text = a.getText();
//        text = text.trim();
        List<Triple<String, Integer, Integer>> list = Load_General.classifier.classifyToCharacterOffsets(text);
        List<GeneralResult> results = new ArrayList<>();

        for (Triple<String, Integer, Integer> item : list) {
            String labelText = item.first();
            String mentionText = text.substring(item.second(), item.third());
            int start = item.second();
            int end = item.third() - 1;
            GeneralResult result = new GeneralResult(start, end, mentionText, labelText);
            results.add(result);
//            System.out.println(item.first() + ": " + text.substring(item.second(), item.third()));
        }
        return results;
    }

    private static List<TmChemResult> process_Chemical(Abstract a) throws IOException {
        String text = a.getText();

//        System.out.println("Text received: " + text);
        if (text == null)
            return new ArrayList<>();
        Map<String, String> abbreviationMap = Load_Chemical.abbrev.getAbbreviations(a.getId(), text);
        List<TmChemResult> found = processText_Chemical(a, abbreviationMap);
        System.out.println("Mentions found:");
        for (TmChemResult result : found)
            System.out.println("\t" + result.toString());
        if (abbreviationMap == null)
            return found;
        // FIXME Consistency
        // FIXME Abbreviation
        return found;
    }

    private static List<TmChemResult> processText_Chemical(Abstract a, Map<String, String> abbreviationMap) {
        List<TmChemResult> results = new ArrayList<>();
        int index = 0;
        List<String> sentences = a.getSentenceTexts();
        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            int length = sentence.length();
            sentence = sentence.trim();
            Sentence sentence1 = new Sentence(a.getId() + "-" + i, a.getId(), sentence);
            Sentence sentence2 = BANNER.process(Load_Chemical.tagger, Load_Chemical.tokenizer, Load_Chemical.postProcessor, sentence1);
            for (Mention mention : sentence2.getMentions(Mention.MentionType.Found)) {
                int start = index + mention.getStartChar();
                int end = start + mention.getText().length();
                TmChemResult result = new TmChemResult(start, end, mention.getText());
                String lookupText = result.getMentionText();
                lookupText = expandAbbreviations(lookupText, abbreviationMap);
                String conceptId = normalize(lookupText);
                result.setConceptID(conceptId);
                results.add(result);
            }
            index += length;
        }
        return results;
    }

    private static List<DNormResult> process_Disease(Abstract a) throws IOException {
        String text = a.getText();
        System.out.println("Text received: " + text);
        if (text == null)
            return new ArrayList<DNormResult>();
        Map<String, String> abbreviationMap = Load_Disease.abbrev.getAbbreviations(a.getId(), text);
        List<DNormResult> found = processText_Disease(a, abbreviationMap);
        System.out.println("Mentions found:");
        for (DNormResult result : found)
            System.out.println("\t" + result.toString());
//        return found;
        List<DNormResult> returned = extendResults(text, found, abbreviationMap);
        System.out.println("Mentions added:");
        List<DNormResult> added = new ArrayList<DNormResult>(returned);
        added.removeAll(found);
        for (DNormResult result : added)
            System.out.println("\t" + result.toString());
//        System.out.println("Mentions removed:");
//        List<DNormResult> removed = new ArrayList<DNormResult>(found);
//        removed.removeAll(returned);
//        for (DNormResult result : removed)
//            System.out.println("\t" + result.toString());
        return returned;
    }

    private static List<DNormResult> processText_Disease(Abstract a, Map<String, String> abbreviationMap) {
        List<DNormResult> results = new ArrayList<DNormResult>();
        int index = 0;
        List<String> sentences = a.getSentenceTexts();
        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            int length = sentence.length();
            sentence = sentence.trim();
            Sentence sentence1 = new Sentence(a.getId() + "-" + i, a.getId(), sentence);
            Sentence sentence2 = BANNER.process(Load_Disease.tagger, Load_Disease.tokenizer, Load_Disease.postProcessor, sentence1);
            for (Mention mention : sentence2.getMentions(Mention.MentionType.Found)) {
                int start = index + mention.getStartChar();
                int end = start + mention.getText().length();
                DNormResult result = new DNormResult(start, end, mention.getText());
                String lookupText = result.getMentionText();
                lookupText = AbbreviationResolver.expandAbbreviations(lookupText, abbreviationMap);
                RankedList<SynonymTrainer.LookupResult> lookup = Load_Disease.syn.lookup(lookupText);
                if (lookup.size() > 0) {
                    result.setConceptID(lookup.getObject(0).getConceptId(), lookup.getValue(0));
                }
                results.add(result);
            }
            index += length;
        }
        return results;
    }

    private static List<DNormResult> extendResults(String text, List<DNormResult> results, Map<String, String> shortLongAbbrevMap) {
        // Get long->short map
        Map<String, String> longShortAbbrevMap = new HashMap<String, String>();
        for (String shortText : shortLongAbbrevMap.keySet()) {
            String longText = shortLongAbbrevMap.get(shortText);
            longShortAbbrevMap.put(longText, shortText);
        }

        // Create a set of strings to be set as results
        Set<DNormResult> unlocalizedResults = new HashSet<DNormResult>();
        for (DNormResult result : results) {
            if (result.getConceptID() != null) {
                unlocalizedResults.add(new DNormResult(-1, -1, result.getMentionText(), result.getConceptID(), result.getScore()));
                if (shortLongAbbrevMap.containsKey(result.getMentionText())) {
                    String mentionText = shortLongAbbrevMap.get(result.getMentionText());
                    //TODO Verify mentionText realistically normalizes to the concept intended, or we will drop the original result
                    unlocalizedResults.add(new DNormResult(-1, -1, mentionText, result.getConceptID(), result.getScore()));
                }
                if (longShortAbbrevMap.containsKey(result.getMentionText())) {
                    String mentionText = longShortAbbrevMap.get(result.getMentionText());
                    unlocalizedResults.add(new DNormResult(-1, -1, mentionText, result.getConceptID(), result.getScore()));
                }
            }
        }

        return localizeResults(text, unlocalizedResults);
    }

    private static List<DNormResult> localizeResults(String text, Set<DNormResult> unlocalizedResults) {
        // Add a result for each instance of a mention found
        List<DNormResult> newResults = new ArrayList<DNormResult>();
        for (DNormResult result : unlocalizedResults) {
            String pattern = "\\b" + Pattern.quote(result.getMentionText()) + "\\b";
            Pattern mentionPattern = Pattern.compile(pattern);
            Matcher textMatcher = mentionPattern.matcher(text);
            while (textMatcher.find()) {
                newResults.add(new DNormResult(textMatcher.start()-1, textMatcher.end()-1, result.getMentionText(), result.getConceptID(), result.getScore()));
            }
        }

        // If two results overlap, remove the shorter of the two
        List<DNormResult> newResults2 = new ArrayList<DNormResult>();
        for (int i = 0; i < newResults.size(); i++) {
            DNormResult result1 = newResults.get(i);
            boolean add = true;
            for (int j = 0; j < newResults.size() && add; j++) {
                DNormResult result2 = newResults.get(j);
                if (i != j && result1.overlaps(result2) && result2.getMentionText().length() > result1.getMentionText().length())
                    add = false;
            }
            if (add)
                newResults2.add(result1);
        }
        return newResults2;
    }

    private static String expandAbbreviations(String lookupText, Map<String, String> abbreviationMap) {
        if (abbreviationMap == null)
            return lookupText;
        for (String abbreviation : abbreviationMap.keySet()) {
            if (lookupText.contains(abbreviation)) {
                String replacement = abbreviationMap.get(abbreviation);
                String updated = null;
                if (lookupText.contains(replacement)) {
                    // Handles mentions like "von Hippel-Lindau (VHL) disease"
                    updated = lookupText.replaceAll("\\(?\\b" + abbreviation + "\\b\\)?", "");
                } else {
                    updated = lookupText.replaceAll("\\(?\\b" + abbreviation + "\\b\\)?", replacement);
                }
                if (!updated.equals(lookupText)) {
                    // System.out.println("Before:\t" + lookupText);
                    // System.out.println("After :\t" + updated);
                    // System.out.println();
                    lookupText = updated;
                }
            }
        }
        return lookupText;
    }

    private static String normalize(String mentionText) {
        String processedText = mentionText.replaceAll("[^A-Za-z0-9]", "");
        String conceptId = Load_Chemical.dict.get(processedText);
        if (conceptId == null) {
            conceptId = "-1";
        }
        return conceptId;
    }


    private static class Final_Results {
        private List<TmChemResult> results_Chemical;
        private List<DNormResult> results_Disease;
        private List<GeneralResult> results_General;
        public Final_Results(List<TmChemResult> results_Chemical, List<DNormResult> results_Disease,
                             List<GeneralResult> results_General) {
            this.results_Chemical = results_Chemical;
            this.results_Disease = results_Disease;
            this.results_General = results_General;
        }
//        public Final_Results(List<TmChemResult> results_Chemical) {
//            this.results_Chemical = results_Chemical;
//            this.results_Disease = null;
//            this.results_General = null;
//        }
        public List<TmChemResult> getResults_Chemical() {
            return results_Chemical;
        }
        public List<DNormResult> getResults_Disease() {
            return results_Disease;
        }
        public List<GeneralResult> getResults_General() { return results_General;}
    }

    private static class TmChemResult {
        private int startChar;
        private int endChar;
        private String mentionText;
        private String conceptID;

        public TmChemResult(int startChar, int endChar, String mentionText) {
            this.startChar = startChar;
            this.endChar = endChar;
            this.mentionText = mentionText;
        }

        public TmChemResult(int startChar, int endChar, String mentionText, String conceptID) {
            this.startChar = startChar;
            this.endChar = endChar;
            this.mentionText = mentionText;
            this.conceptID = conceptID;
        }

        public String getConceptID() {
            return conceptID;
        }

        public void setConceptID(String conceptID) {
            this.conceptID = conceptID;
        }

        public int getStartChar() {
            return startChar;
        }

        public int getEndChar() {
            return endChar - 1;
        }

        public String getMentionText() {
            return mentionText;
        }

        public boolean overlaps(TmChemResult result) {
            return endChar > result.startChar && startChar < result.endChar;
        }

        @Override
        public String toString() {
            return mentionText + " (" + startChar + ", " + endChar + ") -> " + conceptID;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((conceptID == null) ? 0 : conceptID.hashCode());
            result = prime * result + endChar;
            result = prime * result + ((mentionText == null) ? 0 : mentionText.hashCode());
            result = prime * result + startChar;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            TmChemResult other = (TmChemResult) obj;
            if (conceptID == null) {
                if (other.conceptID != null)
                    return false;
            } else if (!conceptID.equals(other.conceptID))
                return false;
            if (endChar != other.endChar)
                return false;
            if (mentionText == null) {
                if (other.mentionText != null)
                    return false;
            } else if (!mentionText.equals(other.mentionText))
                return false;
            if (startChar != other.startChar)
                return false;
            return true;
        }
    }

    private static class DNormResult {
        private int startChar;
        private int endChar;
        private String mentionText;
        private String conceptID;
        private double score;

        public DNormResult(int startChar, int endChar, String mentionText) {
            this.startChar = startChar;
            this.endChar = endChar;
            this.mentionText = mentionText;
        }

        public DNormResult(int startChar, int endChar, String mentionText, String conceptID, double score) {
            this.startChar = startChar;
            this.endChar = endChar;
            this.mentionText = mentionText;
            this.conceptID = conceptID;
            this.score = score;
        }

        public String getConceptID() {
            return conceptID;
        }

        public void setConceptID(String conceptID, double score) {
            this.conceptID = conceptID;
            this.score = score;
        }

        public int getStartChar() {
            return startChar;
        }

        public int getEndChar() {
            return endChar - 1;
        }

        public String getMentionText() {
            return mentionText;
        }

        public double getScore() {
            return score;
        }

        public boolean overlaps(DNormResult result) {
            return endChar > result.startChar && startChar < result.endChar;
        }

        @Override
        public String toString() {
            return mentionText + " (" + startChar + ", " + endChar + ") -> " + conceptID + " @ " + score;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((conceptID == null) ? 0 : conceptID.hashCode());
            result = prime * result + endChar;
            result = prime * result + ((mentionText == null) ? 0 : mentionText.hashCode());
            long temp;
            temp = Double.doubleToLongBits(score);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            result = prime * result + startChar;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            DNormResult other = (DNormResult) obj;
            if (conceptID == null) {
                if (other.conceptID != null)
                    return false;
            } else if (!conceptID.equals(other.conceptID))
                return false;
            if (endChar != other.endChar)
                return false;
            if (mentionText == null) {
                if (other.mentionText != null)
                    return false;
            } else if (!mentionText.equals(other.mentionText))
                return false;
            if (Double.doubleToLongBits(score) != Double.doubleToLongBits(other.score))
                return false;
            if (startChar != other.startChar)
                return false;
            return true;
        }
    }

    private static class GeneralResult {
        private int startChar;
        private int endChar;
        private String mentionText;
        private String label;

        public GeneralResult(int startChar, int endChar, String mentionText, String label) {
            this.startChar = startChar;
            this.endChar = endChar;
            this.mentionText = mentionText;
            this.label = label;
        }

        public int getStartChar() {
            return startChar;
        }
        public int getEndChar() { return endChar;}
        public String getMentionText() {return mentionText;}
        public String getLabel() {return label;}
    }

}
