import org.grouplens.lenskit.vectors.similarity.CosineVectorSimilarity
import org.grouplens.lenskit.vectors.similarity.PearsonCorrelation
import org.grouplens.lenskit.vectors.similarity.VectorSimilarity
import org.lenskit.api.ItemScorer
import org.lenskit.bias.BiasModel
import org.lenskit.bias.ItemBiasModel
import org.lenskit.bias.UserBiasModel
import org.lenskit.knn.NeighborhoodSize
import org.lenskit.knn.item.ItemItemScorer
import org.lenskit.knn.item.model.ItemItemModel
import org.lenskit.knn.user.UserUserItemScorer
import org.lenskit.transform.normalize.BiasUserVectorNormalizer
import org.lenskit.transform.normalize.MeanCenteringVectorNormalizer
import org.lenskit.transform.normalize.UserVectorNormalizer
import org.lenskit.transform.normalize.VectorNormalizer
import org.lenskit.mooc.cbf.LuceneItemItemModel

    algorithm("ItemItemUNorm") {
        attributes["NNbrs"] = 15
        include 'fallback.groovy'
        bind ItemScorer to ItemItemScorer
        bind VectorSimilarity to CosineVectorSimilarity
        set NeighborhoodSize to nnbrs

        bind UserVectorNormalizer to BiasUserVectorNormalizer
        within (UserVectorNormalizer) {
            bind BiasModel to UserBiasModel
        }
    }
    algorithm("ItemItemINorm") {
        attributes["NNbrs"] = nnbrs
        include 'fallback.groovy'
        bind ItemScorer to ItemItemScorer
        bind VectorSimilarity to CosineVectorSimilarity
        set NeighborhoodSize to nnbrs
        bind UserVectorNormalizer to BiasUserVectorNormalizer
        within (UserVectorNormalizer) {
            bind BiasModel to ItemBiasModel
        }
    }
