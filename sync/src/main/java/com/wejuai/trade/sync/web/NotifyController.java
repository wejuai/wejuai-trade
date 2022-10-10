package com.wejuai.trade.sync.web;

import com.wejuai.trade.sync.service.TradeService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author ZM.Wang
 */
@Controller
@RequestMapping("/notify")
public class NotifyController {

    private final TradeService tradeService;

    public NotifyController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @RequestMapping("/weixin/{chargeId}")
    public void weixin(@PathVariable String chargeId, HttpServletRequest request, HttpServletResponse response) {
        tradeService.weixinNotify(chargeId, request, response);
    }

    @RequestMapping("/alipay/{chargeId}")
    public void alipay(@PathVariable String chargeId, HttpServletRequest request, HttpServletResponse response) {
        tradeService.alipayNotify(chargeId, request, response);
    }
}
