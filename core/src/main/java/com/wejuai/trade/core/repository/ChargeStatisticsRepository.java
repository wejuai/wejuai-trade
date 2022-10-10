package com.wejuai.trade.core.repository;

import com.wejuai.entity.mongo.statistics.ChargeStatistics;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChargeStatisticsRepository extends MongoRepository<ChargeStatistics, String> {
}
