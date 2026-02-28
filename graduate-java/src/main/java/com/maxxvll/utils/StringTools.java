package com.maxxvll.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 字符串工具类
 *
 * @author MaxXvll
 * @since 2024-01-01
 */
public class StringTools {

    public static void checkParam(Object param) {
        try {
            Field[] fields = param.getClass().getDeclaredFields();
            boolean notEmpty = false;
            for (Field field : fields) {
                String methodName = "get" + StringTools.upperCaseFirstLetter(field.getName());
                Method method = param.getClass().getMethod(methodName);
                Object object = method.invoke(param);
                if (object != null && object instanceof java.lang.String && !StringTools.isEmpty(object.toString())
                        || object != null && !(object instanceof java.lang.String)) {
                    notEmpty = true;
                    break;
                }
            }
            if (!notEmpty) {
                throw new RuntimeException("多参数更新，删除，必须有非空条件");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("校验参数是否为空失败");
        }
    }

    public static String upperCaseFirstLetter(String field) {
        if (isEmpty(field)) {
            return field;
        }
        //如果第二个字母是大写，第一个字母不大写
        if (field.length() > 1 && Character.isUpperCase(field.charAt(1))) {
            return field;
        }
        return field.substring(0, 1).toUpperCase() + field.substring(1);
    }

    public static boolean isNumber(String str) {
        String checkNumber = "^[0-9]+$";
        if (null == str) {
            return false;
        }
        if (!str.matches(checkNumber)) {
            return false;
        }

        return true;
    }

    public static boolean isEmpty(String str) {
        if (null == str || "".equals(str) || "null".equals(str) || "\u0000".equals(str)) {
            return true;
        } else if ("".equals(str.trim())) {
            return true;
        }
        return false;
    }

    public static final String getRandomNumber(Integer count) {
        return RandomStringUtils.random(count, false, true);
    }

    public static final String getRandomString(Integer count) {
        return RandomStringUtils.random(count, true, true);
    }


    public static String encodeByMD5(String originString) {
        return StringTools.isEmpty(originString) ? null : DigestUtils.md5Hex(originString);
    }

    public static String getFileSuffix(String fileName) {
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        return suffix;
    }

    public static boolean pathIsOk(String path) {
        if (StringTools.isEmpty(path)) {
            return true;
        }
        if (path.contains("../") || path.contains("..\\")) {
            return false;
        }
        return true;
    }


    public static final String getChatSessionId4User(String[] userIds) {
        Arrays.sort(userIds);
        return encodeByMD5(StringUtils.join(userIds, ""));
    }

    public static String cleanHtmlTag(String content) {
        if (isEmpty(content)) {
            return content;
        }
        content = content.replace("<", "&lt;");
        content = content.replace("\r\n", "<br>");
        content = content.replace("\n", "<br>");
        return content;
    }

    public static String resetMessageContent(String content) {
        content = cleanHtmlTag(content);
        return content;
    }

    public static final String getChatSessionId4Group(String groupId) {
        return encodeByMD5(groupId);
    }

    /**
     * 重命名文件（在同名文件后添加时间戳）
     * @param fileName 原文件名
     * @return 重命名后的文件名
     */
    public static String rename(String fileName) {
        if (isEmpty(fileName)) {
            return fileName;
        }
        int lastIndex = fileName.lastIndexOf(".");
        if (lastIndex == -1) {
            return fileName + "_" + System.currentTimeMillis();
        }
        String name = fileName.substring(0, lastIndex);
        String suffix = fileName.substring(lastIndex);
        return name + "_" + System.currentTimeMillis() + suffix;
    }

    /**
     * 获取不含后缀的文件名
     * @param fileName 文件名
     * @return 不含后缀的文件名
     */
    public static String getFileNameNoSuffix(String fileName) {
        if (isEmpty(fileName)) {
            return fileName;
        }
        int lastIndex = fileName.lastIndexOf(".");
        if (lastIndex == -1) {
            return fileName;
        }
        return fileName.substring(0, lastIndex);
    }
}

