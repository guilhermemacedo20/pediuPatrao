package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.PedidoItem;
import com.umc.pediupatrao.entity.StatusPedido;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    public static final int PERIODO_PADRAO_DIAS = 30;
    public static final int PERIODO_MAXIMO_DIAS = 366;
    private static final List<String> STATUS = List.of(
            StatusPedido.RECEBIDO, StatusPedido.EM_PREPARACAO, StatusPedido.PRONTO, StatusPedido.SAIU_PARA_ENTREGA,
            StatusPedido.RETIRADO, StatusPedido.FINALIZADO, StatusPedido.CANCELADO);

    public record Filtro(LocalDate dataInicio, LocalDate dataFim, String status) {
    }

    public record Resumo(LocalDate dataInicio, LocalDate dataFim, BigDecimal faturamento, long totalPedidos,
            long pedidosCancelados, double taxaCancelamento, BigDecimal ticketMedio, long itensVendidos) {
    }

    public record VendaDia(LocalDate data, BigDecimal faturamento, long pedidos) {
    }

    public record PedidosStatus(String status, long quantidade, double percentual) {
    }

    public record ProdutoVendido(String produto, long quantidade, BigDecimal faturamento) {
    }

    private final MongoTemplate mongoTemplate;

    public DashboardService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Filtro criarFiltro(LocalDate dataInicio, LocalDate dataFim, String status) {
        LocalDate fim = dataFim != null ? dataFim : LocalDate.now();
        LocalDate inicio = dataInicio != null ? dataInicio : fim.minusDays(PERIODO_PADRAO_DIAS - 1);
        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException("A data inicial não pode ser maior que a data final.");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) + 1 > PERIODO_MAXIMO_DIAS) {
            throw new IllegalArgumentException("O período não pode passar de " + PERIODO_MAXIMO_DIAS + " dias.");
        }
        String statusNormalizado = null;
        if (status != null && !status.isBlank()) {
            statusNormalizado = status.trim().toUpperCase();
            if (!STATUS.contains(statusNormalizado)) {
                throw new IllegalArgumentException("Status inválido: " + status + ". Use um destes: " + STATUS);
            }
        }
        return new Filtro(inicio, fim, statusNormalizado);
    }

    public Resumo resumo(Filtro filtro) {
        List<Pedido> pedidos = buscarPedidos(filtro);
        long cancelados = pedidos.stream().filter(this::cancelado).count();
        double faturamento = 0;
        long itens = 0;
        for (Pedido pedido : pedidos) {
            if (!cancelado(pedido)) {
                faturamento += valor(pedido);
                itens += quantidadeItens(pedido);
            }
        }
        long validos = pedidos.size() - cancelados;
        return new Resumo(filtro.dataInicio(), filtro.dataFim(), dinheiro(faturamento), pedidos.size(), cancelados,
                percentual(cancelados, pedidos.size()), dinheiro(validos == 0 ? 0 : faturamento / validos), itens);
    }

    public List<VendaDia> vendasPorDia(Filtro filtro) {
        Map<LocalDate, double[]> dias = new LinkedHashMap<>();
        for (LocalDate d = filtro.dataInicio(); !d.isAfter(filtro.dataFim()); d = d.plusDays(1)) {
            dias.put(d, new double[2]);
        }
        for (Pedido pedido : buscarPedidos(filtro)) {
            double[] dia = dias.get(pedido.getDataEntrada().toLocalDate());
            if (dia != null && !cancelado(pedido)) {
                dia[0] += valor(pedido);
                dia[1]++;
            }
        }
        List<VendaDia> resultado = new ArrayList<>();
        dias.forEach((data, v) -> resultado.add(new VendaDia(data, dinheiro(v[0]), (long) v[1])));
        return resultado;
    }

    public List<PedidosStatus> pedidosPorStatus(Filtro filtro) {
        List<Pedido> pedidos = buscarPedidos(filtro);
        Map<String, Long> contagem = new LinkedHashMap<>();
        STATUS.forEach(s -> contagem.put(s, 0L));
        for (Pedido pedido : pedidos) {
            contagem.merge(status(pedido), 1L, Long::sum);
        }
        List<PedidosStatus> resultado = new ArrayList<>();
        contagem.forEach((s, qtd) -> resultado.add(new PedidosStatus(s, qtd, percentual(qtd, pedidos.size()))));
        return resultado;
    }

    public List<ProdutoVendido> produtosMaisVendidos(Filtro filtro, int limite) {
        if (limite < 1 || limite > 50) {
            throw new IllegalArgumentException("O limite deve estar entre 1 e 50.");
        }
        Map<String, double[]> produtos = new HashMap<>();
        for (Pedido pedido : buscarPedidos(filtro)) {
            if (cancelado(pedido) || pedido.getItens() == null) {
                continue;
            }
            double fatorDesconto = 1 - Math.max(0, Math.min(100, pedido.getPercentualDesconto())) / 100;
            for (PedidoItem item : pedido.getItens()) {
                if (item == null || item.getQuantidade() <= 0) {
                    continue;
                }
                String nome = item.getNome() == null || item.getNome().isBlank() ? "Produto sem nome" : item.getNome();
                double[] acumulado = produtos.computeIfAbsent(nome, n -> new double[2]);
                acumulado[0] += item.getQuantidade();
                acumulado[1] += item.getQuantidade() * Math.max(0, item.getPreco()) * fatorDesconto;
            }
        }
        return produtos.entrySet().stream()
                .map(e -> new ProdutoVendido(e.getKey(), (long) e.getValue()[0], dinheiro(e.getValue()[1])))
                .sorted(Comparator.comparingLong(ProdutoVendido::quantidade).reversed()
                        .thenComparing(ProdutoVendido::faturamento, Comparator.reverseOrder()))
                .limit(limite)
                .toList();
    }

    List<Pedido> buscarPedidos(Filtro filtro) {
        LocalDateTime inicio = filtro.dataInicio().atStartOfDay();
        LocalDateTime fim = filtro.dataFim().plusDays(1).atStartOfDay();
        Query query = new Query(Criteria.where("dataEntrada").gte(inicio).lt(fim));
        if (filtro.status() != null) {
            query.addCriteria(Criteria.where("status").is(filtro.status()));
        }
        return mongoTemplate.find(query, Pedido.class).stream()
                .filter(p -> p.getDataEntrada() != null)
                .toList();
    }

    private String status(Pedido pedido) {
        return pedido.getStatus() == null || pedido.getStatus().isBlank()
                ? "SEM_STATUS"
                : pedido.getStatus().trim().toUpperCase();
    }

    private boolean cancelado(Pedido pedido) {
        return StatusPedido.CANCELADO.equals(status(pedido));
    }

    private double valor(Pedido pedido) {
        double valor = pedido.getValorTotal() > 0 ? pedido.getValorTotal() : pedido.getSubtotal() - pedido.getValorDesconto();
        return Math.max(0, valor);
    }

    private long quantidadeItens(Pedido pedido) {
        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            return Math.max(0, pedido.getQuantidade());
        }
        return pedido.getItens().stream().filter(i -> i != null && i.getQuantidade() > 0)
                .mapToLong(PedidoItem::getQuantidade).sum();
    }

    private static BigDecimal dinheiro(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }

    private static double percentual(double parte, double total) {
        return total == 0 ? 0 : BigDecimal.valueOf(parte * 100 / total).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
