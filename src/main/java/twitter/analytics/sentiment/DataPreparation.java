package twitter.analytics.sentiment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class DataPreparation {

   private final static String sourceFile = "/Users/naveennandan/Desktop/twitterdata/training.csv";
   private final static String targetFile = "/Users/naveennandan/Desktop/twitter-sentiment-classifier/"
         + "training-sentiment.csv";

   public static void main(String[] args) {
      try {

         int trainingInstances = 5001;
         int positive = 0;
         int negative = 0;

         BufferedReader br = new BufferedReader(new FileReader(sourceFile));

         BufferedWriter bw = new BufferedWriter(new FileWriter(targetFile));

         String line = null;

         while ((line = br.readLine()) != null) {
            String[] data = line.split(",");
            String annotatedSentiment = data[0];

            if (annotatedSentiment.equals("\"4\"")
                  && positive < trainingInstances) {
               annotatedSentiment = "positive";
               positive++;
            } else if (annotatedSentiment.equals("\"0\"")
                  && negative < trainingInstances) {
               annotatedSentiment = "negative";
               negative++;
            } else {
               continue;
            }

            String tweet = "";

            for (int i = 5; i < data.length; i++) {
               tweet = tweet + data[i].toLowerCase();
            }

            bw.write(annotatedSentiment);
            bw.flush();
            bw.write(",");
            bw.flush();
            bw.write(tweet);
            bw.flush();
            bw.newLine();
            bw.flush();
         }

         bw.close();
         br.close();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
