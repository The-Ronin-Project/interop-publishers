package com.projectronin.interop.publishers.spring

import com.projectronin.interop.datalake.spring.DatalakeSpringConfig
import com.projectronin.interop.kafka.spring.KafkaSpringConfig
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ComponentScan("com.projectronin.interop.publishers")
@Import(DatalakeSpringConfig::class, KafkaSpringConfig::class)
class PublishersSpringConfig
