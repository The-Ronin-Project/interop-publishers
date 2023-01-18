package com.projectronin.interop.publishers.spring

import com.projectronin.interop.aidbox.spring.AidboxSpringConfig
import com.projectronin.interop.datalake.spring.DatalakeSpringConfig
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ComponentScan("com.projectronin.interop.publishers")
@Import(AidboxSpringConfig::class, DatalakeSpringConfig::class)
class PublishersSpringConfig
