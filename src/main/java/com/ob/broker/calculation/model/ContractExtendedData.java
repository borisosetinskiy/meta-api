package com.ob.broker.calculation.model;

import com.ob.broker.common.model.ContractData;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
public class ContractExtendedData extends ContractData {
    final Currency baseCurrency;
    final Currency quoteCurrency;
}
