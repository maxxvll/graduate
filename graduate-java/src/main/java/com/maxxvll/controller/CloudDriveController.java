package com.maxxvll.controller;

import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.utils.MinioUtil;
import com.maxxvll.utils.UserContextUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/cloud")
public class CloudDriveController extends BaseController {

    @Resource
    private MinioUtil minioUtil;

    /**
     * 列出当前用户云盘文件及使用量
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> list() {
        String userId = UserContextUtil.getCurrentUserId();
        try {
            List<Map<String, Object>> files = minioUtil.listCloudFiles(userId);
            // 为前端保留完整 object 名称
            for (Map<String, Object> f : files) {
                String name = (String) f.get("name");
                f.put("object", "cloud/" + userId + "/" + name);
            }
            long used = files.stream().mapToLong(f -> (Long) f.get("size")).sum();
            Map<String, Object> data = new HashMap<>();
            data.put("files", files);
            data.put("used", used);
            return Result.success(data);
        } catch (Exception e) {
            log.error("读取云盘列表失败", e);
            return Result.fail("读取云盘失败");
        }
    }

    /**
     * 上传文件到当前用户云盘
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        String userId = UserContextUtil.getCurrentUserId();
        try {
            String obj = minioUtil.uploadToCloud(file, userId);
            return Result.success(obj);
        } catch (Exception e) {
            log.error("上传云盘失败", e);
            return Result.fail("上传失败");
        }
    }

    /**
     * 从已有链接导入到云盘
     */
    @GetMapping("/download")
    public Result<String> download(@RequestParam String object) {
        try {
            String url = minioUtil.getCloudFileUrl(object);
            return Result.success(url);
        } catch (Exception e) {
            log.error("生成下载链接失败", e);
            return Result.fail("生成下载链接失败");
        }
    }

    @PostMapping("/delete")
    public Result<?> delete(@RequestParam String object) {
        String userId = UserContextUtil.getCurrentUserId();
        try {
            // 仅允许删除自己的云盘文件前缀
            if (object != null && object.startsWith("cloud/" + userId + "/")) {
                minioUtil.removeObject(object);
                return Result.success(null);
            }
            return Result.fail("无权删除");
        } catch (Exception e) {
            log.error("删除云盘文件失败", e);
            return Result.fail("删除失败");
        }
    }

    @PostMapping("/import")
    public Result<String> importUrl(@RequestBody Map<String, String> body) {
        String fileUrl = body == null ? null : body.get("fileUrl");
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return Result.fail("缺少 fileUrl 参数");
        }
        String userId = UserContextUtil.getCurrentUserId();
        try {
            String obj = minioUtil.importToCloudByUrl(fileUrl, userId);
            return Result.success(obj);
        } catch (Exception e) {
            log.error("导入云盘失败", e);
            return Result.fail("导入失败");
        }
    }

}
