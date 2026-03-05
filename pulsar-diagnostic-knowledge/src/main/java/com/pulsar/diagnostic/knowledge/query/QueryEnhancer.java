package com.pulsar.diagnostic.knowledge.query;

/**
 * 查询增强器接口
 *
 * 对用户查询进行重写、扩展和实体提取，提升检索召回率
 */
public interface QueryEnhancer {

    /**
     * 增强查询
     *
     * @param query 原始查询
     * @return 增强后的查询
     */
    EnhancedQuery enhance(String query);

    /**
     * 获取增强器名称
     *
     * @return 增强器名称标识
     */
    String getName();

    /**
     * 检查增强器是否启用
     *
     * @return 是否启用
     */
    default boolean isEnabled() {
        return true;
    }
}