package services.contracts;

import com.google.common.util.concurrent.ListenableFuture;
import models.Kingdom;
import org.json.simple.JSONArray;

import java.util.List;

public interface KingdomService {
    String generateKingdomGeneListUrl(Kingdom kingdom);

    void setInterrupted(boolean interrupted);

    ListenableFuture<List<Kingdom>> createKingdomTrees(List<Kingdom> kingdoms);
}
