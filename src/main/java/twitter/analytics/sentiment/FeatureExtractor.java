package twitter.analytics.sentiment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import redis.clients.jedis.Jedis;
import weka.core.Attribute;
import weka.core.FastVector;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class FeatureExtractor {

   private static MaxentTagger tagger;
   private static Map<Integer, String> tweets;
   private static Map<Integer, String> sentiments;
   private static FastVector attributes;

   public static void extractFeatures(Jedis jedis, String sourceFile)
         throws Exception {

      tweets = new LinkedHashMap<Integer, String>();
      sentiments = new LinkedHashMap<Integer, String>();
      loadData(sourceFile);

      if (jedis.scard("tweet-features") == 0) {
         tagger = new MaxentTagger(
               "taggers/english-bidirectional-distsim.tagger");

         generateFeatures(jedis);
      }
      attributes = new FastVector(jedis.scard("tweet-features").intValue() + 1);
      generateAttributes(jedis);
   }

   public static FastVector getAttributes() {

      return attributes;
   }

   public static Map<Integer, String> getTweets() {
      return tweets;
   }

   public static Map<Integer, String> getSentiments() {
      return sentiments;
   }

   private static void loadData(String sourceFile) throws Exception {
      int count = 1;
      BufferedReader br = new BufferedReader(new FileReader(sourceFile));
      String line = null;

      while ((line = br.readLine()) != null) {
         String[] data = line.split(",");
         String annotatedSentiment = line.split(",")[0];
         String tweet = "";
         for (int i = 1; i < data.length; i++) {
            tweet = tweet + data[i];
         }

         tweets.put(count, tweet.toLowerCase());
         sentiments.put(count, annotatedSentiment);
         count++;
      }
   }

   private static void generateFeatures(Jedis jedis) throws Exception {
      for (int i : tweets.keySet()) {
         String tagged = tagger.tagString(tweets.get(i));
         StringTokenizer st = new StringTokenizer(tagged);

         while (st.hasMoreTokens()) {
            String token = st.nextToken();
            String pos = token.split("/")[1];
            if (pos.startsWith("JJ")) {
               jedis.sadd("tweet-features", token.split("/")[0]);
            }
         }
      }
   }

   private static void generateAttributes(Jedis jedis) throws Exception {

      for (String feature : jedis.smembers("tweet-features")) {
         Attribute attribute = new Attribute(feature);
         attributes.addElement(attribute);
      }

      Set<String> classes = new LinkedHashSet<String>();

      for (int i : sentiments.keySet()) {
         classes.add(sentiments.get(i));
      }

      FastVector classAttribute = new FastVector(classes.size());
      for (String sentiment : classes) {
         classAttribute.addElement(sentiment);
      }

      Attribute sentiment = new Attribute("sentiment", classAttribute);

      attributes.addElement(sentiment);
   }
}
