package com.rainc.kabuto.repository;

/**
 * @author rainc
 * @date 2023/4/10
 */
public interface RegistryRepository {
    boolean hasRegistry(String ip);

    void init(String ip);

    void beat(String ip);

    void maintainActive();

    void remove(String ip);
}
