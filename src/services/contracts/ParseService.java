package services.contracts;

import models.Organism;
import org.jdeferred.Promise;

import java.io.InputStream;
import java.util.List;

public interface ParseService {
    Promise<List<Organism>, Throwable, Void> extractOrganismList(InputStream inputStream, String kingdomId);
}
