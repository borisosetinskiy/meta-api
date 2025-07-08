package com.ob.api.mtx.mt5;


import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Symbols {
    public final Map<String, SymbolSessions> Sessions = new ConcurrentHashMap<>();
    public final Map<String, SymGroup> Groups = new ConcurrentHashMap<>();
    public SymBaseInfo Base;
    public SymGroup[] SymGroups;
    public Map<String, SymbolInfo> Infos = new ConcurrentHashMap<>();

    public final SymbolInfo GetInfo(String symbol) {
        SymbolInfo symbolInfo = symbolInfo(symbol);
        if (symbolInfo != null)
            return symbolInfo;
        else
            throw new CodeException("Symbol not found: " + symbol, Code.NOT_FOUND);
    }

    public final SymbolInfo symbolInfo(String symbol) {
        return Infos.get(symbol);
    }

    public final SymGroup GetGroup(String symbol) {
        if (!Groups.containsKey(symbol)) {
            throw new CodeException("Symbol not found: " + symbol, Code.NOT_FOUND);
        }
        SymGroup res = Groups.get(symbol);
        for (SymGroup slave : SymGroups) {
            String regex = slave.GroupName.replace("\\", "\\\\").replace("*", ".*");
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(res.GroupName);
            if (matcher.matches())
                res.CopyValues(slave);
        }
        return res;
    }

    public final SymbolInfo GetInfo(int id) {
        for (SymbolInfo item : Infos.values()) {
            if (item.Id == id) {
                return item;
            }
        }
        throw new CodeException("Symbol not found: " + id, Code.NOT_FOUND);
    }

    public final boolean Exist(String symbol) {
        return Infos.containsKey(symbol);
    }

    public final String ExistStartsWith(String symbol) {
        for (String name : Infos.keySet()) {
            if (name.startsWith(symbol)) {
                return name;
            }
        }
        return null;
    }
}