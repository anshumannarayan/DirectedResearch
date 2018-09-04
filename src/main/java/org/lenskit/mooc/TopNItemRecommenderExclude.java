package org.lenskit.mooc;
import com.google.common.collect.Ordering;
import it.unimi.dsi.fastutil.longs.*;
import org.lenskit.api.ItemScorer;
import org.lenskit.api.Result;
import org.lenskit.api.ResultList;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemRecommender;
import org.lenskit.basic.TopNItemRecommender;
import org.lenskit.data.dao.ItemDAO;
import org.lenskit.data.dao.UserEventDAO;
import org.lenskit.data.events.Event;
import org.lenskit.data.history.UserHistory;
import org.lenskit.results.Results;
import org.lenskit.util.ScoredIdAccumulator;
import org.lenskit.util.TopNScoredIdAccumulator;
import org.lenskit.util.UnlimitedScoredIdAccumulator;
import org.lenskit.util.collections.LongUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import java.io.FileReader;
import java.util.Set;

import it.unimi.dsi.fastutil.longs.Long2DoubleFunction;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongIterators;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
/*
 * Created by naray190 on 7/4/18.
 */
public class TopNItemRecommenderExclude extends AbstractItemRecommender
{
    private static final Logger logger = LoggerFactory.getLogger(TopNItemRecommenderExclude.class);


    protected final UserEventDAO userEventDAO;
    protected final ItemDAO itemDAO;
    protected final ItemScorer scorer;

    @Inject
    public TopNItemRecommenderExclude(UserEventDAO uedao, ItemDAO idao, ItemScorer scorer) {

        userEventDAO = uedao;
        itemDAO = idao;
        this.scorer = scorer;
    }



    public ItemScorer getScorer() {
        return this.scorer;
    }
    public List<Long> recommend (long user)
    {
        return recommend(user,-1);
    }
    public List<Long> recommend(long user,int n)
    {
        return recommend( user, n);
    }
    public List<Long> recommend(long user, int n, LongSet candidates, LongSet exclude){
        candidates = this.getEffectiveCandidates(user, candidates, exclude);
        logger.debug("Computing {} recommendations for user {} from {} candidates", new Object[]{Integer.valueOf(n), Long.valueOf(user), Integer.valueOf(candidates.size())});
        Map scores = this.scorer.score(user, candidates);
        Object accum;
        if(n >= 0) {
            accum = new TopNScoredIdAccumulator(n);
        } else {
            accum = new UnlimitedScoredIdAccumulator();
        }

        Long2DoubleFunction map = LongUtils.asLong2DoubleFunction(scores);
        LongIterator iter = LongIterators.asLongIterator(scores.keySet().iterator());

        while(iter.hasNext()) {
            long item = iter.nextLong();
            ((ScoredIdAccumulator)accum).put(item, map.get(item));
        }

        return ((ScoredIdAccumulator)accum).finishList();
    }
    protected ResultList recommendWithDetails(long user, int n, LongSet candidates, LongSet exclude){
        candidates = this.getEffectiveCandidates(user, candidates, exclude);
        logger.debug("Computing {} recommendations for user {} from {} candidates", new Object[]{Integer.valueOf(n), Long.valueOf(user), Integer.valueOf(candidates.size())});
        ResultMap scores = this.scorer.scoreWithDetails(user, candidates);
        return this.getTopNResults(n, scores);
    }
    private LongSet getEffectiveCandidates(long user, LongSet candidates, LongSet exclude) {


        if(candidates == null) {
            candidates = this.getPredictableItems(user);
        }

        if(exclude == null) {
            exclude = this.getDefaultExcludes(user);
        }

        logger.debug("computing effective candidates for user {} from {} candidates and {} excluded items", new Object[]{Long.valueOf(user), Integer.valueOf(((LongSet)candidates).size()), Integer.valueOf(exclude.size())});
        if(!exclude.isEmpty()) {
            candidates = LongUtils.setDifference((LongSet)candidates, exclude);
        }

        return (LongSet)candidates;
    }

    protected LongSet getDefaultExcludes(long user) {
        String path="/home/naray190/dr-project/data/exclude_list.tsv";
        LongArraySet excludes = new LongArraySet();
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));

            String line = "";
            while (br.readLine() != null) {
                String[] comp = line.split("\t");
                Long userId = Long.parseLong(comp[0]);
                if (userId == user) {
                    String[] movies = comp[1].split(",");
                    for (String movie : movies) {
                        long movieId = Long.parseLong(movie);
                        excludes.add(movieId);
                    }
                }
            }
        }
        catch(Exception e)
        {
            System.out.print("Exception");
        }
        if(excludes.isEmpty()==false)
        return excludes;
        else
        return this.getDefaultExcludes(this.userEventDAO.getEventsForUser(user));
    }

    protected LongSet getDefaultExcludes(@Nullable UserHistory<? extends Event> user)  {

        return (LongSet)(user == null? LongSets.EMPTY_SET:user.itemSet());
    }

    protected LongSet getPredictableItems(long user) {
        return this.itemDAO.getItemIds();
    }
    private ResultList getTopNResults(int n, Iterable<Result> scores) {
        Ordering ord = Results.scoreOrder();
        Object topN;
        if(n < 0) {
            topN = ord.reverse().immutableSortedCopy(scores);
        } else {
            topN = ord.greatestOf(scores, n);
        }

        return Results.newResultList((List)topN);
    }
    public List<Long> recommend(long user, int n, @Nullable Set<Long> candidates, @Nullable Set<Long> exclude) {
        return recommend(user, n, LongUtils.asLongSet(candidates), LongUtils.asLongSet(exclude));
    }
    public ResultList recommendWithDetails(long user, int n, @Nullable Set<Long> candidates, @Nullable Set<Long> exclude) {
        return recommendWithDetails(user, n, LongUtils.asLongSet(candidates), LongUtils.asLongSet(exclude));
    }

}

