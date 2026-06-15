package com.javedrpi.multitenantservice.config;

import com.javedrpi.multitenantservice.service.DynamicRoutingDataSource;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Central wiring for multi-tenant persistence:
 * - Two named DataSource beans (one per tenant schema)
 * - A DynamicRoutingDataSource wired as @Primary so all JPA operations route through it
 * - Single EntityManagerFactory and TransactionManager backed by the routing datasource
 *
 * Tenant identification happens at JDBC connection time via DynamicRoutingDataSource,
 * which consults TenantContext (set per-request by TenantResolverFilter).
 */
@Configuration
@EnableTransactionManagement
public class HibernateConfig {

    private final Environment env;

    public HibernateConfig(Environment env) {
        this.env = env;
    }

    // ========== 1. Named DataSource beans (one per tenant) ==========

    @Bean(name = "mydbDs")
    public DataSource mydbDataSource() {
        return DataSourceBuilder.create()
                .url(env.getProperty("multitenancy.tenant1.datasource.url"))
                .username(env.getProperty("multitenancy.tenant1.datasource.username"))
                .password(env.getProperty("multitenancy.tenant1.datasource.password"))
                .driverClassName("org.mariadb.jdbc.Driver")
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "mydb2Ds")
    public DataSource mydb2DataSource() {
        return DataSourceBuilder.create()
                .url(env.getProperty("multitenancy.tenant2.datasource.url"))
                .username(env.getProperty("multitenancy.tenant2.datasource.username"))
                .password(env.getProperty("multitenancy.tenant2.datasource.password"))
                .driverClassName("org.mariadb.jdbc.Driver")
                .type(HikariDataSource.class)
                .build();
    }

    // ========== 2. Dynamic routing datasource (PRIMARY) ==========

    @Primary
    @Bean(name = "routingDataSource")
    public DataSource routingDataSource() {
        DynamicRoutingDataSource router = new DynamicRoutingDataSource();

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("tenant1", mydbDataSource());
        targetDataSources.put("tenant2", mydb2DataSource());

        // Fallback used during startup when TenantContext is not yet set
        router.setDefaultTargetDataSource(mydbDataSource());
        router.setTargetDataSources(targetDataSources);

        return router;
    }

    // ========== 3. EntityManagerFactory wired to routing datasource ==========

    @Primary
    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("routingDataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("com.javedrpi.multitenantservice.model");
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Map<String, Object> jpaProps = new HashMap<>();
        jpaProps.put("hibernate.ddl-auto", env.getProperty("spring.jpa.hibernate.ddl-auto", "update"));
        jpaProps.put("hibernate.id.new_generator_mappings", true);
        jpaProps.put("hibernate.physical_naming_strategy",
//                org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy.class.getName()
                NamingStrat.class.getName()
        );

        emf.setJpaPropertyMap(jpaProps);

        return emf;
    }

    // ========== 4. TransactionManager ==========

    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager txManager = new JpaTransactionManager();
        txManager.setEntityManagerFactory(entityManagerFactory);
        return txManager;
    }
}
