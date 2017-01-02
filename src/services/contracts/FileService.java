package services.contracts;

import com.google.common.util.concurrent.ListenableFuture;
import models.Gene;
import models.Kingdom;
import models.Organism;
import models.Sum;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.tree.TreeModel;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface FileService {
    ListenableFuture<TreeModel> buildTree(String path);

    XSSFWorkbook createWorkbook();

    XSSFSheet fillWorkbook(Organism organism, Gene gene, XSSFWorkbook workbook);

    void writeWorkbook(XSSFWorkbook workbook, String path, String fileName) throws IOException;
    
    Map<String,Gene> readWorkbooks(Map<String,Gene> map, File excel, int retry);

    Map<String, Date> readUpdateFile(Kingdom kingdom) throws IOException;

    void writeUpdateFile(Kingdom kingdom, Map<String, Date> updates) throws IOException;

    List<Boolean> createDirectories(List<String> paths);

    XSSFWorkbook fillWorkbookSum(Organism organism, HashMap<String, Sum> organismSums, XSSFWorkbook workbook);
}
