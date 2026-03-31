package com.maxxvll.common.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * XSS 请求包装器
 * <p>
 * 对请求参数进行 XSS 过滤，防止跨站脚本攻击
 * </p>
 *
 * <p><b>过滤内容:</b></p>
 * <ul>
 *     <li>&lt;script&gt; 标签及其内容</li>
 *     <li>javascript: 伪协议</li>
 *     <li>on* 事件处理器（如 onclick、onerror）</li>
 *     <li>HTML 特殊字符转义</li>
 * </ul>
 *
 * @author Claude Code
 * @since 2026-03-31
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    /**
     * 构造函数
     */
    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }

        String[] encodedValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            encodedValues[i] = cleanXss(values[i]);
        }

        return encodedValues;
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return cleanXss(value);
    }

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return cleanXss(value);
    }

    /**
     * 清理 XSS 攻击代码
     *
     * @param value 原始值
     * @return 清理后的值
     */
    private String cleanXss(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // 1. 移除危险的 HTML 标签
        value = removeDangerousTags(value);

        // 2. 移除 javascript: 伪协议
        value = removeJavaScriptProtocol(value);

        // 3. 移除事件处理器
        value = removeEventHandlers(value);

        // 4. 转义特殊字符
        value = escapeSpecialChars(value);

        return value;
    }

    /**
     * 移除危险的 HTML 标签
     */
    private String removeDangerousTags(String value) {
        if (value == null) {
            return null;
        }

        // 移除 <script> 标签及其内容
        value = value.replaceAll("(?i)<script[^>]*>.*?</script>", "");
        value = value.replaceAll("(?i)<script[^>]*>", "");
        value = value.replaceAll("(?i)</script>", "");

        // 移除 <iframe> 标签
        value = value.replaceAll("(?i)<iframe[^>]*>.*?</iframe>", "");
        value = value.replaceAll("(?i)<iframe[^>]*>", "");
        value = value.replaceAll("(?i)</iframe>", "");

        // 移除 <object> 标签
        value = value.replaceAll("(?i)<object[^>]*>.*?</object>", "");
        value = value.replaceAll("(?i)<object[^>]*>", "");
        value = value.replaceAll("(?i)</object>", "");

        // 移除 <embed> 标签
        value = value.replaceAll("(?i)<embed[^>]*>", "");

        // 移除 <meta> 标签（可能用于 HTTP 注入）
        value = value.replaceAll("(?i)<meta[^>]*>", "");

        // 移除 <link> 标签
        value = value.replaceAll("(?i)<link[^>]*>", "");

        // 移除 <style> 标签
        value = value.replaceAll("(?i)<style[^>]*>.*?</style>", "");
        value = value.replaceAll("(?i)<style[^>]*>", "");
        value = value.replaceAll("(?i)</style>", "");

        return value;
    }

    /**
     * 移除 javascript: 伪协议
     */
    private String removeJavaScriptProtocol(String value) {
        if (value == null) {
            return null;
        }

        // 移除 javascript: 伪协议
        value = value.replaceAll("(?i)javascript:", "");

        // 移除 vbscript: 伪协议
        value = value.replaceAll("(?i)vbscript:", "");

        // 移除 data: 协议中的脚本
        value = value.replaceAll("(?i)data:(?!image/.*?;base64).*?", "");

        return value;
    }

    /**
     * 移除事件处理器
     */
    private String removeEventHandlers(String value) {
        if (value == null) {
            return null;
        }

        // 移除 on* 事件处理器
        value = value.replaceAll("(?i)on\\w+\\s*=\\s*[\"'].*?[\"']", "");
        value = value.replaceAll("(?i)on\\w+\\s*=\\s*[^\\s>]*", "");
        value = value.replaceAll("(?i)on\\w+", "");

        return value;
    }

    /**
     * 转义特殊字符
     */
    private String escapeSpecialChars(String value) {
        if (value == null) {
            return null;
        }

        // 转义 HTML 特殊字符
        value = value.replace("&", "&amp;");
        value = value.replace("<", "&lt;");
        value = value.replace(">", "&gt;");
        value = value.replace("\"", "&quot;");
        value = value.replace("'", "&#x27;");
        value = value.replace("/", "&#x2F;");

        return value;
    }
}
