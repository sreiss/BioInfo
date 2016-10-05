package services.contracts;

import models.Organism;
import org.jdeferred.Promise;

import java.io.InputStream;
import java.util.List;

public interface ParseService {
    Promise<List<Organism>, Throwable, Object> extractOrganismList(InputStream inputStream, String kingdomId);
    Promise<List<String>, Throwable, Object> extractSequences(final InputStream inputStream);
}
