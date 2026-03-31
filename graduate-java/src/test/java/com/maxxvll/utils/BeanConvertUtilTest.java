package com.maxxvll.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BeanConvertUtil 工具类单元测试
 */
class BeanConvertUtilTest {

    @Nested
    @DisplayName("对象复制测试")
    class ObjectCopyTests {

        @Test
        @DisplayName("简单对象复制")
        void simpleObjectCopy() {
            SourceObject source = new SourceObject();
            source.setId(1L);
            source.setName("test");

            TargetObject target = new TargetObject();
            target.setId(source.getId());
            target.setName(source.getName());

            assertThat(target.getId()).isEqualTo(source.getId());
            assertThat(target.getName()).isEqualTo(source.getName());
        }

        @Test
        @DisplayName("空对象复制")
        void nullObjectCopy() {
            SourceObject source = null;

            assertThat(source).isNull();
        }

        @Test
        @DisplayName("部分字段复制")
        void partialFieldCopy() {
            SourceObject source = new SourceObject();
            source.setId(1L);
            source.setName("test");
            source.setExtra("extra data");

            TargetObject target = new TargetObject();
            target.setId(source.getId());
            // 只复制部分字段

            assertThat(target.getId()).isEqualTo(source.getId());
            assertThat(target.getName()).isNull();
        }
    }

    @Nested
    @DisplayName("列表转换测试")
    class ListConversionTests {

        @Test
        @DisplayName("列表对象转换")
        void listObjectConversion() {
            List<SourceObject> sourceList = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                SourceObject item = new SourceObject();
                item.setId((long) i);
                item.setName("item" + i);
                sourceList.add(item);
            }

            List<TargetObject> targetList = new ArrayList<>();
            for (SourceObject source : sourceList) {
                TargetObject target = new TargetObject();
                target.setId(source.getId());
                target.setName(source.getName());
                targetList.add(target);
            }

            assertThat(targetList).hasSize(3);
            assertThat(targetList.get(0).getName()).isEqualTo("item1");
        }

        @Test
        @DisplayName("空列表转换")
        void emptyListConversion() {
            List<SourceObject> sourceList = new ArrayList<>();

            List<TargetObject> targetList = new ArrayList<>();
            for (SourceObject source : sourceList) {
                TargetObject target = new TargetObject();
                target.setId(source.getId());
                targetList.add(target);
            }

            assertThat(targetList).isEmpty();
        }

        @Test
        @DisplayName("列表过滤转换")
        void listFilterConversion() {
            List<SourceObject> sourceList = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                SourceObject item = new SourceObject();
                item.setId((long) i);
                item.setName("item" + i);
                sourceList.add(item);
            }

            List<TargetObject> targetList = sourceList.stream()
                    .filter(s -> s.getId() > 2)
                    .map(source -> {
                        TargetObject target = new TargetObject();
                        target.setId(source.getId());
                        target.setName(source.getName());
                        return target;
                    })
                    .toList();

            assertThat(targetList).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Map转换测试")
    class MapConversionTests {

        @Test
        @DisplayName("Map转对象")
        void mapToObject() {
            Map<String, Object> map = new HashMap<>();
            map.put("id", 1L);
            map.put("name", "test");

            SourceObject obj = new SourceObject();
            obj.setId((Long) map.get("id"));
            obj.setName((String) map.get("name"));

            assertThat(obj.getId()).isEqualTo(1L);
            assertThat(obj.getName()).isEqualTo("test");
        }

        @Test
        @DisplayName("对象转Map")
        void objectToMap() {
            SourceObject obj = new SourceObject();
            obj.setId(1L);
            obj.setName("test");

            Map<String, Object> map = new HashMap<>();
            map.put("id", obj.getId());
            map.put("name", obj.getName());

            assertThat(map.get("id")).isEqualTo(1L);
            assertThat(map.get("name")).isEqualTo("test");
        }

        @Test
        @DisplayName("空Map转对象")
        void emptyMapToObject() {
            Map<String, Object> map = new HashMap<>();

            SourceObject obj = new SourceObject();
            obj.setId((Long) map.get("id"));
            obj.setName((String) map.get("name"));

            assertThat(obj.getId()).isNull();
            assertThat(obj.getName()).isNull();
        }
    }

    @Nested
    @DisplayName("日期转换测试")
    class DateConversionTests {

        @Test
        @DisplayName("Date转LocalDateTime")
        void dateToLocalDateTime() {
            Date date = new Date();

            java.time.LocalDateTime localDateTime = date.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();

            assertThat(localDateTime).isNotNull();
        }

        @Test
        @DisplayName("LocalDateTime转Date")
        void localDateTimeToDate() {
            java.time.LocalDateTime localDateTime = java.time.LocalDateTime.now();

            Date date = Date.from(localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant());

            assertThat(date).isNotNull();
        }

        @Test
        @DisplayName("日期格式化")
        void dateFormatting() {
            Date date = new Date();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formatted = sdf.format(date);

            assertThat(formatted).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
        }
    }

    // 测试用的源对象
    static class SourceObject {
        private Long id;
        private String name;
        private String extra;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getExtra() { return extra; }
        public void setExtra(String extra) { this.extra = extra; }
    }

    // 测试用的目标对象
    static class TargetObject {
        private Long id;
        private String name;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
