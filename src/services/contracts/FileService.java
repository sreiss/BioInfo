package services.contracts;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jdeferred.Promise;
import org.jdeferred.multiple.MasterProgress;
import org.jdeferred.multiple.MultipleResults;
import org.jdeferred.multiple.OneReject;

import java.util.List;

public interface FileService {
    Promise<MultipleResults, OneReject, MasterProgress> createDirectories(List<String> paths);
    Promise<Boolean, Throwable, Void> createDirectory(String path);
    Promise<XSSFWorkbook, Throwable, Void> createWorkbook();
    Promise<Void, Throwable, Void> writeWorkbook(XSSFWorkbook workbook, String path, String fileName);
}
