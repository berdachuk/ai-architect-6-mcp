package com.example.medicalmcp.core.repository.sql;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

@Component
public class SqlInjectBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        for (Field field : bean.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(InjectSql.class)) {
                continue;
            }
            String filePath = field.getAnnotation(InjectSql.class).value();
            try {
                ClassPathResource resource = new ClassPathResource(filePath);
                if (!resource.exists()) {
                    throw new BeanCreationException(
                            beanName,
                            "SQL file not found: " + filePath + " for field " + field.getName());
                }
                String sql = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                sql = sql.trim().replace("\r\n", "\n").replace('\r', '\n');
                field.setAccessible(true);
                ReflectionUtils.setField(field, bean, sql);
            } catch (IOException ex) {
                throw new BeanCreationException(beanName, "Failed to load SQL file: " + filePath, ex);
            }
        }
        return bean;
    }
}
