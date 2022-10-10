package com.wejuai.trade.core;

import com.wejuai.trade.core.config.TradeConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author YQ.Huang
 */
@Configuration
@Import(TradeConfig.class)
public class CoreModule {
}
