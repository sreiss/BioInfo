package services.contracts;

import com.google.api.client.http.HttpResponse;
import models.Gene;
import models.Kingdom;
import models.Organism;
import org.jdeferred.Promise;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

public interface KingdomService {
    String generateKingdomGeneListUrl(Kingdom kingdom);
    Promise<List<Kingdom>, Throwable, Object> createKingdomTrees(List<Kingdom> kingdoms, List<InputStream> inputStreams);
    Promise<Kingdom, Throwable, Object> createKingdomTree(Kingdom kingdom, InputStream inputStream);
    Promise<List<Kingdom>, Throwable, Object> processGenes(List<Kingdom> kingdoms);
    Promise<Kingdom, Throwable, Object> processGenes(Kingdom kingdom);
}
