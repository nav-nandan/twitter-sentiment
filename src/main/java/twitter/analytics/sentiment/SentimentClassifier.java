package twitter.analytics.sentiment;

import java.io.BufferedReader;
import java.io.FileReader;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.SparseInstance;

public class SentimentClassifier extends FeatureExtractor {

   private final static String trainingFile = "/Users/naveennandan/Desktop/twitter-sentiment-classifier/"
         + "training-sentiment.csv";
   private static Instances trainingSet;
   private static Instances testSet;
   private static Classifier classifier;
   private final static String testFile = "/Users/naveennandan/Desktop/twitter-sentiment-classifier/"
         + "test-sentiment.csv";
   private final static String classifierPath = "/Users/naveennandan/Desktop/twitter-classifier.ser";
   private static JedisPool pool;
   private static Jedis jedis;

   private static void initialize() throws Exception {
      pool = new JedisPool("localhost");
      jedis = pool.getResource();
   }

   private static void loadClassifier() throws Exception {
      try {
         classifier = (Classifier) SerializationHelper.read(classifierPath);
      } catch (Exception e) {
         buildClassifier();
      }
   }

   public static void main(String[] args) {
      try {
         initialize();
         // extract features from training set
         FeatureExtractor.extractFeatures(jedis, trainingFile);

         // load classifier if present, else build a new classifier
         // loads serialized classifier if present
         loadClassifier();

         // test set - sentiment prediction
         testSet = new Instances("tweet-sentiment-test",
               FeatureExtractor.getAttributes(), 10);
         testSet.setClassIndex(jedis.scard("tweet-features").intValue());

         BufferedReader br = new BufferedReader(new FileReader(testFile));
         String line = null;
         int tp = 0;
         int tn = 0;
         int fp = 0;
         int fn = 0;

         while ((line = br.readLine()) != null) {
            String[] data = line.toLowerCase().split(",");
            String annotatedSentiment = data[0];

            if (!(annotatedSentiment.equals("positive") || annotatedSentiment
                  .equals("negative")))
               continue;

            String tweet = "";
            for (int i = 1; i < data.length; i++) {
               tweet = tweet + data[i];
            }
            Instance instance = buildInstance(testSet, tweet,
                  annotatedSentiment);

            double pred = classifier.classifyInstance(instance);
            String predictedSentiment = testSet.classAttribute().value(
                  (int) pred);

            if (predictedSentiment.equals(annotatedSentiment)) {
               if (predictedSentiment.equals("positive"))
                  tp++;
               else
                  tn++;
            } else {
               if (predictedSentiment.equals("positive"))
                  fp++;
               else
                  fn++;
            }
         }

         // classifier statistics
         double precision = (double) tp / (double) (tp + fp);
         double recall = (double) tp / (double) (tp + fn);
         double accuracy = (double) (tp + tn) / (double) (tp + tn + fp + fn);
         double Fscore = (double) (2 * precision * recall)
               / (double) (precision + recall);

         System.out.println("true positive : " + tp);
         System.out.println("true negative : " + tn);
         System.out.println("false positive : " + fp);
         System.out.println("false negative : " + fn);
         System.out.println("precision : " + precision);
         System.out.println("recall : " + recall);
         System.out.println("F-measure : " + Fscore);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private static Instance buildInstance(Instances dataSet, String tweet,
         String sentiment) throws Exception {
      Instance instance = new SparseInstance(jedis.scard("tweet-features")
            .intValue() + 1);

      int index = 0;

      for (String feature : jedis.smembers("tweet-features")) {
         if (tweet.contains(feature)) {
            instance.setValue((Attribute) FeatureExtractor.getAttributes()
                  .elementAt(index), 1);
         } else {
            instance.setValue((Attribute) FeatureExtractor.getAttributes()
                  .elementAt(index), 0);
         }

         index++;
      }

      instance.setValue(
            (Attribute) FeatureExtractor.getAttributes().elementAt(
                  jedis.scard("tweet-features").intValue()), sentiment);

      instance.setDataset(dataSet);

      return instance;
   }

   private static void buildClassifier() throws Exception {
      trainingSet = new Instances("tweet-sentiment-training",
            FeatureExtractor.getAttributes(), 10);
      trainingSet.setClassIndex(jedis.scard("tweet-features").intValue());

      for (int i : FeatureExtractor.getTweets().keySet()) {
         Instance instance = buildInstance(trainingSet, FeatureExtractor
               .getTweets().get(i), FeatureExtractor.getSentiments().get(i));

         if (instance != null) {
            trainingSet.add(instance);
         }
      }
      classifier = new NaiveBayes();
      classifier.buildClassifier(trainingSet);
      SerializationHelper.write(classifierPath, classifier);
   }
}
