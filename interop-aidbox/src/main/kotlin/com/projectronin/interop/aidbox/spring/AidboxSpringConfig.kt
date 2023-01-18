package com.projectronin.interop.aidbox.spring

import com.projectronin.interop.common.http.spring.HttpSpringConfig
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ComponentScan("com.projectronin.interop.aidbox")
@Import(HttpSpringConfig::class)
class AidboxSpringConfig
