package mandarin.com.mandarin_backend.util;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Component
public class FileUtil {

    private final String uploadDir = "uploads/character/";

    public String saveFile(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) return null;

        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        File saveFile = new File(uploadDir + fileName);
        file.transferTo(saveFile);

        return uploadDir + fileName;
    }

    public void deleteFile(String path) {
        if (path == null) return;
        File file = new File(path);
        if (file.exists()) file.delete();
    }
}
