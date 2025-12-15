package mandarin.com.mandarin_backend.util;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Component
public class FileUtil {

    // [수정 1] 절대 경로 사용 (EC2 서버의 프로젝트 실행 위치/uploads/ 로 잡힘)
    private final String BASE_DIR = System.getProperty("user.dir") + "/uploads/";

    /**
     * 파일 저장
     * @param file 업로드할 파일
     * @param subDirectory 저장할 하위 폴더 이름 (예: "character", "dialogue")
     * @return DB에 저장될 상대 경로
     */
    public String saveFile(MultipartFile file, String subDirectory) throws IOException {

        if (file == null || file.isEmpty()) return null;

        // [수정 2] 기본 경로 + 하위 폴더 경로 결합
        String savePath = BASE_DIR + subDirectory;
        File dir = new File(savePath);

        // [수정 3] 폴더가 없으면 생성 (mkdirs는 상위 폴더까지 다 만들어줌)
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                System.out.println("디렉토리 생성됨: " + savePath);
            }
        }

        // 파일명 생성 (UUID + 원본명)
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        
        // 실제 저장
        File saveFile = new File(savePath + "/" + fileName);
        file.transferTo(saveFile);

        // DB에는 "character/파일이름.jpg" 형태로 저장
        return subDirectory + "/" + fileName;
    }

    /**
     * 파일 삭제
     * @param path DB에 저장된 상대 경로 (예: "character/abc.jpg")
     */
    public void deleteFile(String path) {
        if (path == null || path.isEmpty()) return;
        
        // 절대 경로로 변환하여 삭제
        File file = new File(BASE_DIR + path);
        if (file.exists()) file.delete();
    }
}
