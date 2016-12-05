package services.contracts;

import com.google.common.util.concurrent.ListenableFuture;
import models.Kingdom;
import org.json.simple.JSONArray;

import java.util.List;

public interface KingdomService {

    String generateKingdomGeneListUrl(Kingdom kingdom);

    ListenableFuture<List<Kingdom>> createKingdomTrees(List<Kingdom> kingdoms);

    void interrupt();

    boolean getShouldInterrupt();
    
    void setGenesCkBIsSelected(Boolean isSelected);
    Boolean getGenesCkBIsSelected();
    void setGenomesCkBIsSelected(Boolean isSelected);
    Boolean getGenomesCkBIsSelected();
}
