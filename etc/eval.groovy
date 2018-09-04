import org.lenskit.api.ItemScorer
import org.lenskit.bias.*
import org.lenskit.baseline.BaselineScorer
import org.lenskit.baseline.ItemMeanRatingItemScorer
import org.lenskit.baseline.UserMeanBaseline
import org.lenskit.baseline.UserMeanItemScorer
import org.lenskit.baseline.MeanDamping
import org.lenskit.knn.item.ItemItemScorer
import org.lenskit.transform.normalize.MeanCenteringVectorNormalizer
import org.lenskit.transform.normalize.VectorNormalizer
import org.lenskit.knn.NeighborhoodSize
import org.lenskit.mf.funksvd.*
import org.lenskit.mooc.TopNItemRecommenderExclude
import org.grouplens.lenskit.iterative.IterationCount

for (size in [30,100]){
    algorithm("SVDPersMean") {
        attributes["FeatureCount"] = size

        set IterationCount to 150
        set FeatureCount to size
        bind ItemScorer to FunkSVDItemScorer
        bind (BaselineScorer, ItemScorer) to UserMeanItemScorer
        bind (UserMeanBaseline, ItemScorer) to ItemMeanRatingItemScorer
        set MeanDamping to 5
        //bind ItemRecommender to TopNItemRecommenderExclude

    }
}
algorithm("ItemItem") {
    bind ItemScorer to ItemItemScorer
    bind VectorNormalizer to MeanCenteringVectorNormalizer
    set NeighborhoodSize to 20
}