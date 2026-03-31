package com.maxxvll.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Date;

/**
 * MyBatis-Plus 配置类
 *
 * 配置内容：
 * 1. 分页插件
 * 2. 自动填充处理器（createTime、updateTime）
 *
 * @author backend
 */
@Slf4j
@Configuration
public class MybatisPlusConfig {

    /**
     * MyBatis-Plus 拦截器配置
     * 添加分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页插件（MySQL 数据库）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 自动填充处理器
     *
     * 功能：
     * - 插入操作时自动填充 createTime 和 updateTime
     * - 更新操作时自动更新 updateTime
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                log.trace("开始插入填充...");
                Date now = new Date();
                strictInsertFill(metaObject, "createTime", Date.class, now);
                strictInsertFill(metaObject, "updateTime", Date.class, now);
                strictInsertFill(metaObject, "createdAt", Date.class, now);
                strictInsertFill(metaObject, "updatedAt", Date.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                log.trace("开始更新填充...");
                Date now = new Date();
                strictUpdateFill(metaObject, "updateTime", Date.class, now);
                strictUpdateFill(metaObject, "updatedAt", Date.class, now);
            }
        };
    }
}
