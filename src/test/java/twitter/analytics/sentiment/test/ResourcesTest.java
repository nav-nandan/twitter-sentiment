package twitter.analytics.sentiment.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class ResourcesTest {

   @Test
   public void testPOSTagger() {
      try {
         MaxentTagger tagger = new MaxentTagger(
               "taggers/english-bidirectional-distsim.tagger");

         assertNotNull(tagger);
      } catch (Exception e) {
         fail(e.getMessage());
      }
   }

   @Test
   public void testRedisConnection() {
      try {
         JedisPool pool = new JedisPool("localhost");

         assertNotNull(pool);

         Jedis jedis = pool.getResource();

         assertNotNull(jedis);
      } catch (Exception e) {
         fail(e.getMessage());
      }
   }
}
