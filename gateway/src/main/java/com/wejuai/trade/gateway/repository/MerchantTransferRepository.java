package com.wejuai.trade.gateway.repository;

import com.wejuai.entity.mongo.MerchantTransfer;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author ZM.Wang
 */
public interface MerchantTransferRepository extends MongoRepository<MerchantTransfer, String> {
}
