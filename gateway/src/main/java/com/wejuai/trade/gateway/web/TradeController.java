package com.wejuai.trade.gateway.web;

import com.wejuai.dto.request.RechargeRequest;
import com.wejuai.dto.request.WithdrawalTradeRequest;
import com.wejuai.dto.response.MchTradeResponse;
import com.wejuai.trade.gateway.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author ZM.Wang
 */
@RestController
@RequestMapping("/trade")
public class TradeController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final OrderService orderService;

    public TradeController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/{userId}")
    public Map<String, String> payment(@PathVariable String userId,
                                       @RequestBody RechargeRequest rechargeRequest,
                                       @RequestParam String ip) {
        return orderService.payment(userId, ip, rechargeRequest);
    }

    @PostMapping("/merchantPay")
    public MchTradeResponse merchantPay(@RequestBody WithdrawalTradeRequest request, @RequestParam String ip) {
        return orderService.merchantPay(request, ip);
    }

}
