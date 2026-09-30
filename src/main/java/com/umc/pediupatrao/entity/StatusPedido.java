package com.umc.pediupatrao.entity;

import java.util.Map;
import java.util.Set;

public final class StatusPedido {
    public static final String RECEBIDO = "RECEBIDO";
    public static final String EM_PREPARACAO = "EM_PREPARACAO";
    public static final String PRONTO = "PRONTO";
    public static final String SAIU_PARA_ENTREGA = "SAIU_PARA_ENTREGA";
    public static final String RETIRADO = "RETIRADO";
    public static final String FINALIZADO = "FINALIZADO";
    public static final String CANCELADO = "CANCELADO";

    private static final Map<String, Set<String>> TRANSICOES = Map.of(
            RECEBIDO, Set.of(EM_PREPARACAO),
            EM_PREPARACAO, Set.of(PRONTO),
            PRONTO, Set.of(SAIU_PARA_ENTREGA, RETIRADO),
            SAIU_PARA_ENTREGA, Set.of(FINALIZADO),
            RETIRADO, Set.of(FINALIZADO)
    );

    private StatusPedido() {
    }

    public static boolean podeAvancar(String de, String para) {
        return TRANSICOES.getOrDefault(de, Set.of()).contains(para);
    }

    public static boolean antesDaSaida(String status) {
        return RECEBIDO.equals(status) || EM_PREPARACAO.equals(status) || PRONTO.equals(status);
    }
}
