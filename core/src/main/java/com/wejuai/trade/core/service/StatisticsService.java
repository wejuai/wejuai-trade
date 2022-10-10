package com.wejuai.trade.core.service;

import com.wejuai.entity.mongo.AppType;
import com.wejuai.entity.mongo.statistics.ChargeStatistics;
import com.wejuai.entity.mongo.statistics.ChargeStatisticsType;
import com.wejuai.entity.mongo.statistics.OrdersStatistics;
import com.wejuai.trade.core.repository.ChargeStatisticsRepository;
import com.wejuai.trade.core.repository.OrdersStatisticsRepository;
import org.springframework.stereotype.Service;

import static com.wejuai.entity.mongo.AppType.ARTICLE;
import static com.wejuai.entity.mongo.AppType.REWARD_DEMAND;
import static com.wejuai.entity.mongo.statistics.ChargeStatisticsType.RECHARGE;
import static com.wejuai.entity.mongo.statistics.ChargeStatisticsType.TRANSFER;
import static com.wejuai.entity.mongo.statistics.ChargeStatisticsType.WITHDRAWAL;
import static com.wejuai.trade.core.config.Constant.CHARGE_STATISTICS_ID;
import static com.wejuai.trade.core.config.Constant.ORDERS_STATISTICS_ID;

/**
 * @author ZM.Wang
 */
@Service
public class StatisticsService {

    private final OrdersStatisticsRepository ordersStatisticsRepository;
    private final ChargeStatisticsRepository chargeStatisticsRepository;

    public StatisticsService(OrdersStatisticsRepository ordersStatisticsRepository, ChargeStatisticsRepository chargeStatisticsRepository) {
        this.ordersStatisticsRepository = ordersStatisticsRepository;
        this.chargeStatisticsRepository = chargeStatisticsRepository;
    }

    public void addAppOrders(AppType type, long amount) {
        if (type == ARTICLE) {
            ordersStatisticsRepository
                    .save(getOrders().addArticleCount().addArticleAmount(amount));
        }
        if (type == REWARD_DEMAND) {
            ordersStatisticsRepository
                    .save(getOrders().addRewardDemandCount().addRewardDemandAmount(amount));
        }
    }

    public void addTransferOrders(long amount) {
        ordersStatisticsRepository
                .save(getOrders().addTransferAddCount().addTransferAddAmount(amount));
    }

    public void addCharge(ChargeStatisticsType type, long amount) {
        if (type == RECHARGE) {
            chargeStatisticsRepository
                    .save(getCharge().addRechargeCount().addRechargeAmount(amount));
        }
        if (type == WITHDRAWAL) {
            chargeStatisticsRepository
                    .save(getCharge().addWithdrawalCount().addWithdrawalAmount(amount));
        }
        if (type == TRANSFER) {
            chargeStatisticsRepository
                    .save(getCharge().addTransferCount().addTransferAmount(amount));
        }
    }

    private OrdersStatistics getOrders() {
        return ordersStatisticsRepository
                .findById(ORDERS_STATISTICS_ID)
                .orElse(ordersStatisticsRepository.save(new OrdersStatistics(ORDERS_STATISTICS_ID)));
    }

    private ChargeStatistics getCharge() {
        return chargeStatisticsRepository
                .findById(CHARGE_STATISTICS_ID)
                .orElse(chargeStatisticsRepository.save(new ChargeStatistics(CHARGE_STATISTICS_ID)));
    }
}
