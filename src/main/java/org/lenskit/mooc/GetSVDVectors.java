package org.lenskit.mooc;

import org.apache.commons.math3.linear.RealVector;
import org.grouplens.lenskit.iterative.IterationCount;
import org.lenskit.LenskitConfiguration;
import org.lenskit.LenskitRecommender;
import org.lenskit.api.ItemScorer;
import org.lenskit.baseline.BaselineScorer;
import org.lenskit.baseline.ItemMeanRatingItemScorer;
import org.lenskit.baseline.UserMeanBaseline;
import org.lenskit.baseline.UserMeanItemScorer;
import org.lenskit.baseline.*;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.dao.UserDAO;
import org.lenskit.data.dao.file.StaticDataSource;
import org.lenskit.data.ratings.Rating;
import org.lenskit.mf.funksvd.FeatureCount;
import org.lenskit.mf.funksvd.FunkSVDItemScorer;
import org.lenskit.mf.funksvd.FunkSVDModel;
import org.lenskit.util.io.ObjectStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;

/**
 * This class generates the SVD feature vectors for our set of test users and also calculate the cosine similarity between a user's full rating vector and their popular only rating vector.
 * We also calculate the baseline similarity to contextualize the similarity results.
 */
public class GetSVDVectors {
    private static Logger logger =  LoggerFactory.getLogger(GetSVDVectors.class);
    public static void main(String args[])throws Exception {

            int featureCount =30;// we build our popular and full rating model.
            logger.info("Configuring Lenskit recommender");
            LenskitConfiguration config = new LenskitConfiguration();
            config.bind(ItemScorer.class).to(FunkSVDItemScorer.class);
            config.set(FeatureCount.class).to(featureCount);
            config.set(IterationCount.class).to(150);
            config.bind(BaselineScorer.class,ItemScorer.class).to(UserMeanItemScorer.class);
            config.bind(UserMeanBaseline.class,ItemScorer.class).to(ItemMeanRatingItemScorer.class);
            config.set(MeanDamping.class).to(5);
            config.addRoot(FunkSVDModel.class);
            config.addRoot(UserDAO.class);

            StaticDataSource sample_ratings = StaticDataSource.load(Paths.get("/project/naray190/dr-project/data/fulltrain.yml"));
            DataAccessObject dao = sample_ratings.get();
            logger.info("Building recommender");
            LenskitRecommender rec = LenskitRecommender.build(config,dao);
            logger.info("Recommender built");
            LenskitConfiguration configpop =  new LenskitConfiguration();
            configpop.bind(ItemScorer.class).to(FunkSVDItemScorer.class);
            StaticDataSource pop_ratings = StaticDataSource.load(Paths.get("/project/naray190/dr-project/data/poptrain.yml"));
            DataAccessObject popdao = pop_ratings.get();
            configpop.set(FeatureCount.class).to(featureCount);
            configpop.set(IterationCount.class).to(150);
            configpop.bind(BaselineScorer.class,ItemScorer.class).to(UserMeanItemScorer.class);
            configpop.bind(UserMeanBaseline.class,ItemScorer.class).to(ItemMeanRatingItemScorer.class);
            configpop.set(MeanDamping.class).to(5);
            configpop.addRoot(FunkSVDModel.class);
            configpop.addRoot(UserDAO.class);
            logger.info("Building recommender");
            LenskitRecommender poprec = LenskitRecommender.build(configpop,popdao);
            logger.info("Recommender built");
            
            StaticDataSource test_full = StaticDataSource.load(Paths.get("/project/naray190/dr-project/data/fulltest.yml"));
            DataAccessObject testdao = test_full.get();

            
        ArrayList<Long> candidate_users = new ArrayList<Long>();
       
            logger.info("Collecting sample users");
            try (ObjectStream<Rating> ratings = testdao.query(Rating.class)
                .stream()) {

                for(Rating r : ratings)
                {
                long userId = r.getUserId();
                if( candidate_users.contains(userId)==false) {
                    candidate_users.add(userId);
                    

                }

            }
            logger.info("Collected Sample users");


        }
        logger.info("Accessing group 1 user features");
        FunkSVDModel mod = rec.get(FunkSVDModel.class);
        String filename = "usvdfeaturesg1.tsv";
        OutputStream fileout = new FileOutputStream(filename,true);
        StringBuilder headline = new StringBuilder();
        headline.append("15 features for 100k");
        headline.append("\n");
        headline.append("\n");
        headline.append(new Date().getTime());
        headline.append("Feature Count "+featureCount+"\n");
        fileout.write(headline.toString().getBytes("ASCII"));
        for (long user:candidate_users){
           StringBuilder row = new StringBuilder();
           row.append(user);
           row.append('\t');
           RealVector user_f = mod.getUserVector(user);
           double[] farray= user_f.toArray();
           for (int i=0;i<featureCount;i++){
               row.append(farray[i]);
               row.append(',');



           }
           row.append('\n');
            fileout.write(row.toString().getBytes("ASCII"));
        }
        logger.info("Accessing group 2 user features");
        filename =  "usvdfeaturesg2.tsv";;
        FunkSVDModel popmod = poprec.get(FunkSVDModel.class);
        fileout = new FileOutputStream(filename,true);
        headline = new StringBuilder();
        headline.append("15 features for 100k");
        headline.append("\n");
        headline.append("\n");
        headline.append(new Date().getTime());
        headline.append("Feature Count "+featureCount+"\n");
        fileout.write(headline.toString().getBytes("ASCII"));

        for (long user:candidate_users){
            StringBuilder row = new StringBuilder();
            row.append(user);
            row.append('\t');
            RealVector user_f = popmod.getUserVector(user);
            double[] farray= user_f.toArray();
            for (int i=0;i<featureCount;i++){
                row.append(farray[i]);
                row.append(',');



            }
            row.append('\n');
            fileout.write(row.toString().getBytes("ASCII"));
        }
        //Generating baseline similarity score
        fileout.close();
        filename =  "/project/naray190/dr-project/data/30FeatureRecsDamped/user_sim_score.tsv";
        fileout = new FileOutputStream(filename,true);
        headline = new StringBuilder();
        headline.append("\n");
        headline.append("\n");
        headline.append(new Date().getTime());
        headline.append("Feature Count "+featureCount+"\n");
        fileout.write(headline.toString().getBytes("ASCII"));
        double baseline_sim =0.0;
        double divide_term =0;
        double baseline_sim_2=0.0;
       
        
        for(long user:candidate_users){
            int nextIndex = candidate_users.indexOf(user) +1;
            if (nextIndex==candidate_users.size())
                break;

            RealVector U1=mod.getUserVector(user);
            RealVector U2=mod.getUserVector(candidate_users.get(nextIndex));
            baseline_sim += U1.cosine(U2);
            divide_term++;
            U1=popmod.getUserVector(user);
            U2=popmod.getUserVector(candidate_users.get(nextIndex));
            baseline_sim_2 +=U1.cosine(U2);
        }
        baseline_sim=baseline_sim/divide_term;
        baseline_sim_2=baseline_sim_2/divide_term;
        StringBuilder head = new StringBuilder();
        head.append("Baseline Similarity score for full vectors \t" );
        head.append(baseline_sim);
        head.append('\n');
        head.append("Baseline similiarity score for popular only vectors \t");
        head.append(baseline_sim_2);
        head.append('\n');
        fileout.write(head.toString().getBytes("ASCII"));

        for(long user: candidate_users){
           StringBuilder row= new StringBuilder();
           
           RealVector ug1=mod.getUserVector(user);
           RealVector ug2=popmod.getUserVector(user);
           double sim_score=ug1.cosine(ug2);
           row.append(user);
           row.append('\t');
           row.append(sim_score);
           row.append('\n');
           fileout.write(row.toString().getBytes("ASCII"));


        }
        fileout.close();



    }

}
