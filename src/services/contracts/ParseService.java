package services.contracts;

import com.google.common.util.concurrent.ListenableFuture;
import models.Gene;
import models.Organism;
import java.io.InputStream;
import java.util.List;

public interface ParseService {
    ListenableFuture<List<Organism>> extractOrganismList(InputStream inputStream, String kingdomId);
    ListenableFuture<List<String>> extractSequences(final InputStream inputStream, Gene gene);
}
