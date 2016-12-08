package services.contracts;

import java.io.File;
import com.google.common.util.concurrent.ListenableFuture;

public interface ZipService {

	ListenableFuture<Boolean> createGenomeDirectory(File node);
	
}
