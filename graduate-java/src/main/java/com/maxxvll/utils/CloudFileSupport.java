package com.maxxvll.utils;

import java.net.URLConnection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CloudFileSupport {

    public record Descriptor(
            String extension,
            String contentType,
            String category,
            String previewMode,
            boolean previewable,
            boolean streamable,
            boolean binaryPreview,
            boolean textPreview,
            boolean documentPreview
    ) {
    }

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "webp", "bmp", "svg"
    );
    private static final Set<String> VIDEO_EXTENSIONS = Set.of(
            "mp4", "webm", "mov", "m4v", "ogg", "ogv", "avi", "mkv"
    );
    private static final Set<String> AUDIO_EXTENSIONS = Set.of(
            "mp3", "wav", "ogg", "oga", "m4a", "aac", "flac"
    );
    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            "txt", "md", "markdown", "json", "js", "ts", "tsx", "jsx", "vue",
            "html", "htm", "css", "scss", "less", "xml", "yml", "yaml", "csv",
            "log", "sql", "properties", "conf", "ini", "java", "kt", "py", "go",
            "sh", "bat", "cmd"
    );
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of(
            "doc", "docx", "xls", "xlsx", "ppt", "pptx"
    );
    private static final Set<String> ARCHIVE_EXTENSIONS = Set.of(
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz"
    );
    private static final Map<String, String> MIME_BY_EXTENSION = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("bmp", "image/bmp"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("webm", "video/webm"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("m4v", "video/x-m4v"),
            Map.entry("ogv", "video/ogg"),
            Map.entry("ogg", "audio/ogg"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("wav", "audio/wav"),
            Map.entry("m4a", "audio/mp4"),
            Map.entry("aac", "audio/aac"),
            Map.entry("flac", "audio/flac"),
            Map.entry("txt", "text/plain"),
            Map.entry("md", "text/markdown"),
            Map.entry("markdown", "text/markdown"),
            Map.entry("json", "application/json"),
            Map.entry("html", "text/html"),
            Map.entry("htm", "text/html"),
            Map.entry("css", "text/css"),
            Map.entry("xml", "application/xml"),
            Map.entry("csv", "text/csv"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("zip", "application/zip")
    );

    private CloudFileSupport() {
    }

    public static Descriptor describe(String fileName, String rawContentType) {
        String extension = resolveExtension(fileName);
        String contentType = normalizeContentType(fileName, rawContentType);

        if ("pdf".equals(extension) || "application/pdf".equalsIgnoreCase(contentType)) {
            return new Descriptor(extension, contentType, "document", "pdf", true, false, true, false, false);
        }

        if (IMAGE_EXTENSIONS.contains(extension) || contentType.startsWith("image/")) {
            return new Descriptor(extension, contentType, "image", "image", true, false, true, false, false);
        }

        if (VIDEO_EXTENSIONS.contains(extension) || contentType.startsWith("video/")) {
            return new Descriptor(extension, contentType, "video", "video", true, true, false, false, false);
        }

        if (AUDIO_EXTENSIONS.contains(extension) || contentType.startsWith("audio/")) {
            return new Descriptor(extension, contentType, "audio", "audio", true, true, false, false, false);
        }

        if (DOCUMENT_EXTENSIONS.contains(extension)) {
            return new Descriptor(extension, contentType, "document", "document", true, false, false, false, true);
        }

        if (TEXT_EXTENSIONS.contains(extension)
                || contentType.startsWith("text/")
                || "application/json".equalsIgnoreCase(contentType)
                || "application/xml".equalsIgnoreCase(contentType)) {
            return new Descriptor(extension, contentType, "document", "text", true, false, false, true, false);
        }

        if (ARCHIVE_EXTENSIONS.contains(extension)) {
            return new Descriptor(extension, contentType, "archive", "unsupported", false, false, false, false, false);
        }

        return new Descriptor(extension, contentType, "file", "unsupported", false, false, false, false, false);
    }

    public static String normalizeContentType(String fileName, String rawContentType) {
        String contentType = String.valueOf(rawContentType == null ? "" : rawContentType).trim();
        if (!contentType.isEmpty() && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            return contentType;
        }

        String extension = resolveExtension(fileName);
        if (MIME_BY_EXTENSION.containsKey(extension)) {
            return MIME_BY_EXTENSION.get(extension);
        }

        String guessed = URLConnection.guessContentTypeFromName(fileName);
        if (guessed != null && !guessed.isBlank()) {
            return guessed;
        }

        return "application/octet-stream";
    }

    public static String resolveExtension(String fileName) {
        String normalized = String.valueOf(fileName == null ? "" : fileName).trim();
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == normalized.length() - 1) {
            return "";
        }
        return normalized.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
