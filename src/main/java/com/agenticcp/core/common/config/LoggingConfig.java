package com.agenticcp.core.common.config;

import com.agenticcp.core.common.logging.MdcProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MdcProperties.class)
public class LoggingConfig {
}
