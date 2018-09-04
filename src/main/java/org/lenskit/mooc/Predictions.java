package org.lenskit.mooc;

import org.grouplens.lenskit.iterative.IterationCount;
import org.lenskit.LenskitConfiguration;
import org.lenskit.LenskitRecommender;
import org.lenskit.api.ItemRecommender;
import org.lenskit.api.ItemScorer;
import org.lenskit.baseline.*;
import org.lenskit.basic.TopNItemRecommender;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.dao.file.StaticDataSource;
import org.lenskit.data.ratings.Rating;
import org.lenskit.mf.funksvd.FeatureCount;
import org.lenskit.mf.funksvd.FunkSVDItemScorer;
import org.lenskit.util.io.ObjectStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.*;
import java.nio.file.Paths;
import java.util.*;

/**
 * Class that handles all recommendation tasks including generating recommendations for our test set of users with both models. The class builds the model anew everytime.
 */
public class Predictions {
    private static Logger logger = LoggerFactory.getLogger(Predictions.class);
    public static void main(String args[])throws Exception {
        logger.info("Configuring Lenskit recommender");//building both of our models(full and popular only)
        LenskitConfiguration config = new LenskitConfiguration();
        config.bind(ItemScorer.class).to(FunkSVDItemScorer.class);
        config.set(FeatureCount.class).to(30);
        config.set(IterationCount.class).to(150);
        config.bind(BaselineScorer.class,ItemScorer.class).to(UserMeanItemScorer.class);
        config.bind(UserMeanBaseline.class,ItemScorer.class).to(ItemMeanRatingItemScorer.class);
        config.set(MeanDamping.class).to(5);
        StaticDataSource sample_ratings = StaticDataSource.load(Paths.get("/home/naray190/eval-assignment/data/fulltrain.yml"));

        DataAccessObject dao =  sample_ratings.get();
        logger.info("Building recommender");
        LenskitRecommender rec = LenskitRecommender.build(config,dao);
        logger.info("extracting candidate users");
	StaticDataSource test_ratings = StaticDataSource.load(Paths.get("/home/naray190/eval-assignment/data/fulltest.yml"));
        ArrayList<Long> candidate_users = new ArrayList<Long>();
	DataAccessObject testdao = test_ratings.get();
        
        ItemRecommender irec = rec.getItemRecommender();
        try (ObjectStream<Rating> ratings = testdao.query(Rating.class)
                .stream()) {

            for(Rating r : ratings)
            {
                long userId = r.getUserId();
                if(candidate_users.contains(userId)==false) {
                    candidate_users.add(userId);
                    

                }

            }

        }
	LenskitConfiguration config2 = new LenskitConfiguration();
        config2.bind(ItemScorer.class).to(FunkSVDItemScorer.class);
        config2.set(FeatureCount.class).to(30);
        config2.set(IterationCount.class).to(150);
        config2.bind(BaselineScorer.class,ItemScorer.class).to(UserMeanItemScorer.class);
        config2.bind(UserMeanBaseline.class,ItemScorer.class).to(ItemMeanRatingItemScorer.class);

        StaticDataSource pop_sample_ratings = StaticDataSource.load(Paths.get("/home/naray190/eval-assignment/data/poptrain.yml"));

        DataAccessObject popdao =  pop_sample_ratings.get();
        logger.info("Building recommender");
        LenskitRecommender poprec = LenskitRecommender.build(config2,popdao);
	ItemRecommender popirec = poprec.getItemRecommender();


        String path = "/project/naray190/dr-project/data/30FeatureRecsDamped/recsfull.tsv";
        String path2 =  "/project/naray190d/dr-project/data/30FeatureRecsDamped/recspop.tsv";

      getPredictionswithLimits(candidate_users,irec,testdao,path);
        getPredictionswithLimits(candidate_users,popirec,testdao,path2);
        String path3 = "/project/naray190/dr-project/data/30FeatureRecsDamped/statrecoverlapnos.tsv";
        comparePredictions(candidate_users,popirec,irec,path3,testdao);
      /** String path4="/project/naray190/eval-assignment/data/30FeatureRecsDamped/allrecmovie.tsv";
        popularVectorRecommendations(candidate_users,irec,path4);
        String path5="/project/naray190/eval-assignment/data/30FeatureRecsDamped/poprecmovie.tsv";
        popularVectorRecommendations(candidate_users,popirec,path5);
       **?

    }

    /**
     *
     * Obsolete recommend function
    private static void getPredictions(ArrayList<Long> users,ItemRecommender irec,String pathname) throws IOException
    {
        OutputStream out = new FileOutputStream(pathname,true);
        out.write(("\n\n\n\n").getBytes());

        for (long user: users){
		
            List<Long > items = irec.recommend(user,20);
            StringBuilder row = new StringBuilder();
            row.append(user);
            row.append('\t');
            int count=0;
            for (long item:items) {
                row.append(item);
                if(count!=19)
                row.append(',');
                count++;

            }
            row.append('\n');
            out.write(row.toString().getBytes("ASCII"));

        }
        out.close();


    */


    }
    private static void getPredictionswithLimits(ArrayList<Long> users,ItemRecommender irec,DataAccessObject dao,String pathname) throws IOException// recommendation function that excludes the user's full rating profile movies. candidates are null which default
                                                                                                                                                    // to all movies. These are stored a tsv file with a user and their top 20 recommendations.
    {
        OutputStream out = new FileOutputStream(pathname,true);
        out.write(("UserId\tMovies\n").getBytes());

        for (long user: users){
		Set item_set = new HashSet<Long>();

	    try (ObjectStream<Rating> ratings = dao.query(Rating.class)
                .stream()) {

           	 for(Rating r : ratings)
           	 {
                long userId = r.getUserId();
			if(userId == user)
			{
				if(item_set.contains(r.getItemId())==false)
				{
					item_set.add(r.getItemId());
				}
			}
                

                }

          	  }

       		 
            List<Long > items = irec.recommend(user,20,null,item_set);
            StringBuilder row = new StringBuilder();
            row.append(user);
            row.append('\t');
            int count=0;
            for (long item:items) {
                row.append(item);
                if(count!=19)
                    row.append(',');
                count++;


            }
            row.append('\n');
            out.write(row.toString().getBytes("ASCII"));

        }
        out.close();





    }
    private static void comparePredictions(ArrayList<Long> users, ItemRecommender popirec, ItemRecommender irec, String pathname,DataAccessObject dao) throws IOException// this function compares the recommendations generated by the two models for our test users and calculates the overlap in recommendation
    {
        OutputStream out= new FileOutputStream(pathname,true);
	
       // out.write("\n\n\n\nUser and percentage overlap in top-20 recommendations \n".getBytes());
	//out.write("Comparing predictions with the user's ratings excluded\n".getBytes());
        Set overlapping_movies = new HashSet();
        HashMap<Long,Integer> rec_movie_count = new HashMap<>();
        HashMap<Long,Integer> rec_pop_movie_count = new HashMap<>();
        HashMap<Long,Integer> overlap_rec_count = new HashMap<>();
        out.write("UserId,Overlap\n".getBytes());
        for (long user:users)
        {
	    Set user_movies = new HashSet<Long>();

	    try (ObjectStream<Rating> ratings = dao.query(Rating.class)
                .stream()) {

           	 for(Rating r : ratings)
           	 {
                long userId = r.getUserId();
			if(userId == user)
			{
				if(user_movies.contains(r.getItemId())==false)
				{
					user_movies.add(r.getItemId());
				}
			}
                

                }

          	  }
            List<Long> items_u= irec.recommend(user,20,null,user_movies);
            
            List<Long> items_su = popirec.recommend(user,20,null,user_movies);
            for (long movie:items_u)
            {
                int count=0;
                if(rec_movie_count.containsKey(movie)==true)
                {
                    count=rec_movie_count.get(movie);
                    count++;
                    rec_movie_count.put(movie,count);
                }
                else
                    rec_movie_count.put(movie,1);// we store the number of times a movie is recommended to a user. This is done for the normal recommendation, the popular rating user recommendation

            }
            for(long movie:items_su)
            {
                int count=0;
                if(rec_pop_movie_count.containsKey(movie)==true)
                {
                    count=rec_pop_movie_count.get(movie);
                    count++;
                    rec_pop_movie_count.put(movie,count);
                }
                else
                    rec_pop_movie_count.put(movie,1);
            }

            Set item_set = new HashSet(items_u);
            Set item_set_stump = new HashSet(items_su);
            Set intersection_set = new HashSet(item_set);
            Set difference_set = new HashSet(item_set);



            intersection_set.retainAll(item_set_stump);

            int overlap = intersection_set.size();
		
            StringBuilder row = new StringBuilder();
            row.append(user);
            row.append(",");
            
            
            row.append(overlap);
            row.append("\n");

            out.write(row.toString().getBytes("ASCII"));
            difference_set.removeAll(item_set_stump);
      /**
            Iterator<Long> itr = difference_set.iterator();
            row = new StringBuilder();
            while(itr.hasNext())
            {
                int count=0;
                Long item = itr.next();
                row.append(item);
                row.append(" , ");
                if(overlapping_movies.contains(item)==false) {
                    overlapping_movies.add(item);
                    overlap_rec_count.put(item,1);
                }
                else
                {
                    count = overlap_rec_count.get(item);
                    count++;
                    overlap_rec_count.put(item,count);
                }


            }
            row.append("\n");
             out.write(row.toString().getBytes("ASCI/**I"));

		**/

        }
        Iterator it = overlapping_movies.iterator();
        StringBuilder row = new StringBuilder();
        while(it.hasNext())
        {
            row.append(it.next());
            row.append(",");

        }
        row.append("\n");
        out.write(row.toString().getBytes("ASCII"));
        out.write("\n\n\n".getBytes("ASCII"));
        out.close();
        OutputStream out2 = new FileOutputStream("/project/naray190/eval-assignment/data/30FeatureRecsDamped/all_movie_count.tsv",true);
        out2.write("MovieId\tCount\n".getBytes());
        for(long item:rec_movie_count.keySet())
        {
             row= new StringBuilder();
            row.append(item);
            row.append("\t");
            row.append(rec_movie_count.get(item));
            row.append("\n");
            out2.write(row.toString().getBytes("ASCII"));
        }
        out2.close();
        out = new FileOutputStream("/project/naray190/eval-assignment/data/30FeatureRecsDamped/pop_movie_count.tsv",true);
        out.write("MovieId\tCount\n".getBytes());
        for(long item:rec_pop_movie_count.keySet())
        {
            row= new StringBuilder();
            row.append(item);
            row.append("\t");
            row.append(rec_pop_movie_count.get(item));
            row.append("\n");
            out.write(row.toString().getBytes("ASCII"));
        }
        out.close();
        /**out = new FileOutputStream("/project/naray190/eval-assignment/data/30FeatureRecsDamped/overlap_movie_count.tsv",true);
        out.write("MovieId\tCount\n".getBytes());

        for(long item:overlap_rec_count.keySet())
        {
            row = new StringBuilder();
            row.append(item);
            row.append("\t");
            row.append(overlap_rec_count.get(item));
            row.append("\n");
            out.write(row.toString().getBytes());
        }
        out.close();
*/


    
    }
    /**
    private static void popularVectorRecommendations(ArrayList<Long> users, ItemRecommender popirec, String pathname) throws IOException
    {
    	OutputStream out = new FileOutputStream(pathname,true);
    	out.write("UserId\tRecommendation\n".getBytes());
    	Set pop_movies = new HashSet();
    	for(long user: users)
    	{
    		List<Long> items = popirec.recommend(user,20);
    		Set item_set =  new HashSet(items);
    		Iterator<Long> itr=item_set.iterator();
    		while(itr.hasNext())
    		{
    			Long item=itr.next();
    			if(pop_movies.contains(item)==false)
    				pop_movies.add(item);

    		}


    	}
    	Iterator it = pop_movies.iterator();
    		StringBuilder row = new StringBuilder();
    		while(it.hasNext())
    		{
    			row.append(it.next());
    			row.append(",");

    		}
    		row.append("\n");
    		out.write(row.toString().getBytes("ASCII"));
    		out.close();
    }
*/
}
