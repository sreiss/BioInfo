package services.contracts;

import com.google.common.util.concurrent.ListenableFuture;
import models.Gene;

public interface StatisticsService {
    ListenableFuture<Gene> computeStatistics(Gene gene);
}
