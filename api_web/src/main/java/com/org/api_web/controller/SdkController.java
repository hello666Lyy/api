package com.org.api_web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * SDK下载接口
 */
@RestController
@RequestMapping("/api/sdk")
public class SdkController {
    
    private static final Logger log = LoggerFactory.getLogger(SdkController.class);

    /**
     * 下载SDK jar包
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadSdk() {
        try {
            // 获取项目根目录（从user.dir获取）
            String userDir = System.getProperty("user.dir");
            log.info("当前工作目录: {}", userDir);
            
            // 尝试多个可能的路径
            String[] possiblePaths = {
                userDir + File.separator + "api_sdk" + File.separator + "target" + File.separator + "api_sdk-1.0-SNAPSHOT.jar",  // 从项目根目录（绝对路径）
                userDir + File.separator + ".." + File.separator + "api_sdk" + File.separator + "target" + File.separator + "api_sdk-1.0-SNAPSHOT.jar",  // 如果user.dir是api_web目录
                "api_sdk" + File.separator + "target" + File.separator + "api_sdk-1.0-SNAPSHOT.jar",  // 相对路径1
                ".." + File.separator + "api_sdk" + File.separator + "target" + File.separator + "api_sdk-1.0-SNAPSHOT.jar"  // 相对路径2
            };
            
            File jarFile = null;
            for (String pathStr : possiblePaths) {
                try {
                    Path path = Paths.get(pathStr).normalize().toAbsolutePath();
                    File file = path.toFile();
                    log.debug("尝试路径: {} -> 存在: {}", pathStr, file.exists());
                    if (file.exists() && file.isFile()) {
                        jarFile = file;
                        log.info("找到SDK jar包: {}", jarFile.getAbsolutePath());
                        break;
                    }
                } catch (Exception e) {
                    log.warn("路径检查失败: {}, 错误: {}", pathStr, e.getMessage());
                    // 继续尝试下一个路径
                    continue;
                }
            }
            
            if (jarFile == null || !jarFile.exists()) {
                // 如果找不到jar包，返回404，并记录错误信息
                log.error("SDK jar包未找到，尝试的路径：");
                for (String pathStr : possiblePaths) {
                    log.error("  - {}", pathStr);
                }
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(jarFile);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"api_sdk-1.0-SNAPSHOT.jar\"");
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(jarFile.length());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取SDK版本信息
     */
    @GetMapping("/version")
    public ResponseEntity<Map<String, String>> getSdkVersion() {
        Map<String, String> version = new HashMap<>();
        version.put("version", "1.0-SNAPSHOT");
        version.put("groupId", "com.org");
        version.put("artifactId", "api_sdk");
        version.put("description", "API Platform SDK for Java");
        return ResponseEntity.ok(version);
    }
}

