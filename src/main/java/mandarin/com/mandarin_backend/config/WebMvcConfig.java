package mandarin.com.mandarin_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. 연결할 URL 패턴: /uploads/로 시작하는 모든 요청
        // 2. 실제 파일 위치: 프로젝트 루트 폴더 밑의 uploads 폴더
        // (file: 접두어와 끝에 /가 꼭 있어야 합니다!)
        String uploadPath = "file:" + System.getProperty("user.dir") + "/uploads/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }
}
