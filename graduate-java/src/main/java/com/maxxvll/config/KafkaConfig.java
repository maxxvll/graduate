package com.maxxvll.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.BatchMessagingMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 通用配置（支持泛型事件）
 * <p>
 * 支持多种事件类型的序列化和反序列化，不再硬编码为单一事件类型
 * </p>
 *
 * <p><b>核心改进:</b></p>
 * <ul>
 *     <li>使用 Object 类型替代 ChatMessageEvent，支持所有事件类型</li>
 *     <li>保留批量消费能力，提升吞吐量</li>
 *     <li>支持 Spring Boot 自动配置的 JSON 序列化器</li>
 *     <li>信任所有包（生产环境应限制为 com.maxxvll.common.event）</li>
 * </ul>
 *
 * @author Claude Code
 * @since 2026-03-17（重构：支持泛型事件）
 */
@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.properties.spring.json.trusted.packages:*}")
    private String trustedPackages;

    /**
     * 批处理消费者工厂（通用）
     * <p>
     * 用于批量处理 Kafka 消息，支持所有事件类型
     * </p>
     */
    @Bean(name = "batchKafkaListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> batchKafkaListenerFactory(
            ConsumerFactory<String, Object> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);

        // 使用 CommonErrorHandler 替代已废弃的 BatchLoggingErrorHandler
        CommonErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(1000L, 3));
        factory.setCommonErrorHandler(errorHandler);

        // 配置批处理消息转换器
        factory.setBatchMessageConverter(new BatchMessagingMessageConverter(converter()));

        return factory;
    }

    /**
     * 单条消息消费者工厂（通用）
     * <p>
     * 用于非批处理场景，支持所有事件类型
     * </p>
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        return factory;
    }

    /**
     * 消费者工厂配置（通用）
     * <p>
     * 使用 Object 类型支持所有事件类型
     * </p>
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // 基础配置
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "default-consumer-group");

        // 批处理配置（关键优化）
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "500");  // 每批最多 500 条消息
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, "200");  // 最多等待 200ms
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, "1024");   // 最少拉取 1KB
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, "10485760"); // 最多拉取 10MB

        // 反序列化配置
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // JSON 反序列化配置（信任所有包）
        props.put(JsonDeserializer.TRUSTED_PACKAGES, trustedPackages != null ? trustedPackages : "*");

        // 类型映射（与生产者一致）
        props.put(JsonDeserializer.TYPE_MAPPINGS,
            "event:com.maxxvll.common.event.LoginEvent," +
            "chat:com.maxxvll.common.event.ChatMessageEvent," +
            "email:com.maxxvll.common.event.EmailEvent," +
            "friend:com.maxxvll.common.event.FriendApplicationEvent"
        );

        // 其他配置
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * 生产者工厂配置（通用）
     * <p>
     * 使用 Object 类型支持所有事件类型
     * </p>
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();

        // 基础配置
        props.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // 序列化配置
        props.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringSerializer.class);
        props.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                org.springframework.kafka.support.serializer.JsonSerializer.class);

        // 性能优化配置
        props.put(org.apache.kafka.clients.producer.ProducerConfig.BATCH_SIZE_CONFIG, "16384");  // 16KB 批处理
        props.put(org.apache.kafka.clients.producer.ProducerConfig.LINGER_MS_CONFIG, "10");      // 10ms 延迟
        props.put(org.apache.kafka.clients.producer.ProducerConfig.BUFFER_MEMORY_CONFIG, "33554432"); // 32MB 缓冲
        props.put(org.apache.kafka.clients.producer.ProducerConfig.RETRIES_CONFIG, "3");
        props.put(org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG, "1");

        // JsonSerializer 类型映射（支持多种事件类型）
        // 只映射实际存在的类
        props.put(JsonSerializer.TYPE_MAPPINGS,
            "event:com.maxxvll.common.event.LoginEvent," +
            "chat:com.maxxvll.common.event.ChatMessageEvent," +
            "email:com.maxxvll.common.event.EmailEvent," +
            "friend:com.maxxvll.common.event.FriendApplicationEvent"
        );

        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * Kafka 模板（通用）
     * <p>
     * 支持所有事件类型
     * </p>
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory);
        template.setMessageConverter(new StringJsonMessageConverter());
        return template;
    }

    /**
     * JSON 消息转换器
     */
    @Bean
    public StringJsonMessageConverter converter() {
        return new StringJsonMessageConverter();
    }
}
