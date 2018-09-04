import org.lenskit.api.ItemScorer
import org.lenskit.basic.PopularityRankItemScorer
import org.lenskit.mooc.TopNItemRecommenderExclude
import org.lenskit.bias.*


algorithm("BiasScorer") {
    // score items by their popularity
    bind ItemScorer to BiasItemScorer
    bind BiasModel to UserItemBiasModel
    // rating prediction is meaningless for this algorithm

    bind ItemRecommender to TopNItemRecommenderExclude
}