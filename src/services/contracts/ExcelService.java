package services.contracts;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jdeferred.Promise;
import org.jdeferred.multiple.MasterProgress;

public interface ExcelService {
    Promise<XSSFWorkbook, Throwable, Void> createWorkbook();
    Promise<Void, Throwable, MasterProgress> writeWorkbook(XSSFWorkbook workbook, String path, String fileName);
}
