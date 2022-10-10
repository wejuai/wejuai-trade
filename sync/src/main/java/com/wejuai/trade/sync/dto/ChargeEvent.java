package com.wejuai.trade.sync.dto;


/**
 * @author YQ.Huang
 */
public class ChargeEvent {

    private final String chargeId;

    public ChargeEvent(String chargeId) {
        this.chargeId = chargeId;
    }

    public String getChargeId() {
        return chargeId;
    }
}