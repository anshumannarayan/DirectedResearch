package org.lenskit.mooc;
import org.grouplens.lenskit.iterative.IterationCount;
import org.lenskit.LenskitConfiguration;
import org.lenskit.LenskitRecommender;
import org.lenskit.api.ItemRecommender;
import org.lenskit.api.ItemScorer;
import org.lenskit.baseline.*;
import org.lenskit.basic.TopNItemRecommender;
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
import org.apache.commons.math3.linear.RealVector;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
/**
 * Created by naray190 on 8/23/18.
 * This class to view the detailed recommendations for the first 50 set of users and a fixed set of movies to view their vectors and get their dot product.
 */
public class DetailPredictions {
    private static Logger logger = LoggerFactory.getLogger(Predictions.class);
    public static void main(String[] args) throws Exception{

        int featureCount =100;
        logger.info("Configuring Lenskit recommender");
        LenskitConfiguration config = new LenskitConfiguration();
        config.bind(ItemScorer.class).to(FunkSVDItemScorer.class);
        config.set(FeatureCount.class).to(featureCount);
        config.set(IterationCount.class).to(150);
        config.bind(BaselineScorer.class,ItemScorer.class).to(UserMeanItemScorer.class);
        config.bind(UserMeanBaseline.class,ItemScorer.class).to(ItemMeanRatingItemScorer.class);
        config.addRoot(FunkSVDModel.class);
        config.addRoot(UserDAO.class);
        config.set(MeanDamping.class).to(5);
        StaticDataSource sample_ratings = StaticDataSource.load(Paths.get("/project/naray190/dr-project/data/fulltrain.yml"));
        DataAccessObject dao = sample_ratings.get();
        logger.info("Building recommender");
        LenskitRecommender rec = LenskitRecommender.build(config,dao);// building the regular recommender model
        logger.info("Recommender built");
        LenskitConfiguration configpop =  new LenskitConfiguration();
        configpop.bind(ItemScorer.class).to(FunkSVDItemScorer.class);
        StaticDataSource pop_ratings = StaticDataSource.load(Paths.get("/project/naray190/dr-project/data/poptrain.yml"));
        DataAccessObject popdao = pop_ratings.get();
        configpop.set(FeatureCount.class).to(featureCount);
        configpop.set(IterationCount.class).to(150);
        configpop.bind(BaselineScorer.class,ItemScorer.class).to(UserMeanItemScorer.class);
        configpop.bind(UserMeanBaseline.class,ItemScorer.class).to(ItemMeanRatingItemScorer.class);
        configpop.addRoot(FunkSVDModel.class);
        configpop.addRoot(UserDAO.class);
        configpop.set(MeanDamping.class).to(5);
        FunkSVDModel fmod=rec.get(FunkSVDModel.class);
        logger.info("Building recommender");
        LenskitRecommender poprec = LenskitRecommender.build(configpop,popdao);//building the popular ratings recommender model.
        logger.info("Recommender built");
        FunkSVDModel fpmod=poprec.get(FunkSVDModel.class);
        OutputStream out = new FileOutputStream("/project/naray190/dr-project/data/100FeatureRecsDamped/detailpredictfull.tsv",false);
        long[] users=new long[]{42273,42577,42779,43130,43256,43450,43606,43831,43853,43966,44038,44363,44447,44454,44807,45101,45259,45999,46629,47230,47701,47944,47993,48351,48934,49212,49796,50330,51444,51875,51890,51939,51966,51982,52146,53023,53834,54018,54441,54449,54466,55448,55701,55768,55951,56103,56116,56291,56427};
        long[] movies=new long[]{159135,159125,159471,159123,110108,159133,151989,146489,169816,147966,129223,146487,169820,133013,160028,122535,138172,160030,169818,177209};
        StringBuilder headline= new StringBuilder("Detailed Predictions for 50 users and 20 Movies using 100 Features(Full Model)\n");
        headline.append("UserId\tItemId\tUVector\tIVector\tDotProduct\n");
        out.write(headline.toString().getBytes("ASCII"));

        for (long user:users){//printing the user and item vectors and calculating their dot product from the normal model(full ratings for test users)
            StringBuilder row=new StringBuilder();
            row.append(user+"\t");
            RealVector uvec= fmod.getUserVector(user);
           double[] uarr=uvec.toArray();
            for(long movie:movies){
                RealVector ivec=fmod.getItemVector(movie);
                row.append(movie+"\t");
                double iarr[]=ivec.toArray();
                String ustr= Arrays.toString(uarr);
                String istr=Arrays.toString(iarr);
                row.append(ustr+"\t");
                row.append(istr+"\t");
                double product=uvec.dotProduct(ivec);
                row.append(product+"\n");
            }
            out.write(row.toString().getBytes("ASCII"));

        }

        out.close();
        out = new FileOutputStream("/project/naray190/dr-project/data/100FeatureRecsDamped/detailpredictpop.tsv",false);
         headline= new StringBuilder("Detailed Predictions for 50 users and 20 Movies using 100 Features(Popular ratings only model)\n");
        headline.append("UserId\tItemId\tUVector\tIVector\tDotProduct\n");
        out.write(headline.toString().getBytes("ASCII"));

        for (long user:users){//printing the user and item vectors and calculating their dot product from the popular only model(popular only ratings for the test users)
            StringBuilder row=new StringBuilder();
            row.append(user+"\t");
            RealVector uvec= fmod.getUserVector(user);
            double[] uarr=uvec.toArray();
            for(long movie:movies){
                RealVector ivec=fpmod.getItemVector(movie);
                row.append(movie+"\t");
                double iarr[]=ivec.toArray();
                String ustr= Arrays.toString(uarr);
                String istr=Arrays.toString(iarr);
                row.append(ustr+"\t");
                row.append(istr+"\t");
                double product=uvec.dotProduct(ivec);
                row.append(product+"\n");
            }
            out.write(row.toString().getBytes("ASCII"));

        }

    }
}
