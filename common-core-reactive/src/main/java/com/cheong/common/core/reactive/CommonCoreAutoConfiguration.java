package com.cheong.common.core.reactive;

import com.cheong.common.core.reactive.handler.GlobalReactiveExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@Import(GlobalReactiveExceptionHandler.class)
public class CommonCoreAutoConfiguration {

}
