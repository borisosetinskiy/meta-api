package com.ob.api.dx.service;

import com.ob.api.dx.model.UserSession;
import com.ob.api.dx.model.response.InstrumentResponse;
import com.ob.api.dx.util.ConnectionUtil;
import com.ob.api.dx.util.HttpUtil;
import com.ob.broker.common.JsonService;
import com.ob.broker.common.client.rest.RestApiClient;
import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import com.ob.broker.common.model.ContractData;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.ob.broker.util.Util.countDecimalDigits;

@Data
@Slf4j
public class InstrumentService {
    final String url;
    final UserSession userSession;
    final RestApiClient restApiClient;
    final Map<String, ContractData> instruments = new ConcurrentHashMap<>();

    public void load(Type type) {
        if (ConnectionUtil.isConnected(userSession.getConnectionStatus().get())) {
            try {
                var url = getInstrumentUrl(type.name());
                HttpUtil.get(url, userSession, restApiClient, responseMessage -> {
                    InstrumentResponse instrumentResponse = JsonService.JSON.fromJson(responseMessage.message(), InstrumentResponse.class);
                    parse(instrumentResponse);
                    log.info("Instrument {} result: {}", type, responseMessage);
                });
            } catch (Exception e) {
                log.error("Error Instrument {}", type, e);
                throw new CodeException(e.getMessage(), Code.INSTRUMENT_LOAD_ERROR);
            }
        }
    }

    private String getInstrumentUrl(String type) {
        return url + "/instruments/query?types=" + type;
    }

    private void parse(InstrumentResponse instrumentResponse) {
        var instruments = instrumentResponse.getInstruments();
        for (var instrument : instruments) {
            var symbol = instrument.getSymbol();
            var contractData = new ContractData();
            contractData.setSymbol(symbol);
            contractData.setCurrency(instrument.getCurrency());
            contractData.setWorkSessions(Map.of());
            contractData.setContractType(InstrumentResponse.toContractType(instrument.getType()));
            contractData.setContractSize(BigDecimal.valueOf(instrument.getLotSize()));
            contractData.setPointSize(BigDecimal.valueOf(instrument.getPipSize()));
            contractData.setDigits(countDecimalDigits(contractData.getPointSize()));
            contractData.setMarginDivider(BigDecimal.valueOf(instrument.getMultiplier()));
            this.instruments.put(contractData.getSymbol(), contractData);
        }
    }

    public enum Type {
        FOREX,
        CFD,
        FUTURES
    }
}
