package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.PedidoItem;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    private Pedido buscar(String id) {
        return pedidoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado"));
    }

    public Pedido criarPedido(Pedido pedido) {
        pedido.setId(null);
        pedido.setResponsavelEntrada(
                SecurityContextHolder.getContext().getAuthentication().getName());
        pedido.setDataEntrada(LocalDateTime.now());
        pedido.setPercentualDesconto(0);
        pedido.setValorDesconto(0);
        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new IllegalArgumentException("O pedido precisa de ao menos um item.");
        }
        double subtotal = 0;
        List<PedidoItem> calculados = new ArrayList<>();
        for (PedidoItem informado : pedido.getItens()) {
            if (informado.getId() == null || informado.getId().isBlank()) {
                throw new IllegalArgumentException("Selecione um produto.");
            }
            if (informado.getQuantidade() < 1) {
                throw new IllegalArgumentException("Quantidade inválida.");
            }
            Produto produto = produtoRepository.findById(informado.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));
            if (produto.getPreco() == null) {
                throw new IllegalArgumentException("Produto sem preço cadastrado.");
            }
            PedidoItem item = new PedidoItem();
            item.setId(produto.getId());
            item.setNome(produto.getNome());
            item.setQuantidade(informado.getQuantidade());
            item.setPreco(produto.getPreco());
            calculados.add(item);
            subtotal += item.getQuantidade() * item.getPreco();
        }
        pedido.setItens(calculados);
        pedido.setSubtotal(subtotal);
        pedido.setValorTotal(subtotal - pedido.getValorDesconto());
        pedido.setStatus(StatusPedido.RECEBIDO);
        return pedidoRepository.save(pedido);
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    public Pedido atualizarStatus(String id, String novoStatus) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado"));
        if (StatusPedido.CANCELADO.equals(pedido.getStatus())) {
            throw new IllegalArgumentException("Pedido cancelado não pode ser alterado.");
        }
        if (!StatusPedido.podeAvancar(pedido.getStatus(), novoStatus)) {
            throw new IllegalArgumentException("Transição inválida: " + pedido.getStatus() + " para " + novoStatus);
        }
        pedido.setStatus(novoStatus);
        if (StatusPedido.SAIU_PARA_ENTREGA.equals(novoStatus) || StatusPedido.RETIRADO.equals(novoStatus)) {
            pedido.setDataSaida(LocalDateTime.now());
            pedido.setResponsavelSaida(
                    SecurityContextHolder.getContext().getAuthentication().getName());
        }
        return pedidoRepository.save(pedido);
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, double percentual) {
        if (percentual <= 0 || percentual > 20) {
            throw new IllegalArgumentException("Desconto permitido de 0,01% a 20%.");
        }
        Pedido pedido = buscar(id);
        if (!StatusPedido.antesDaSaida(pedido.getStatus())) {
            throw new IllegalArgumentException("Desconto só antes da saída ou retirada.");
        }
        double valorDesconto = pedido.getSubtotal() * percentual / 100.0;
        pedido.setPercentualDesconto(percentual);
        pedido.setValorDesconto(valorDesconto);
        pedido.setValorTotal(pedido.getSubtotal() - valorDesconto);
        Pedido salvo = pedidoRepository.save(pedido);
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelar(String id, String justificativa) {
        if (justificativa == null || justificativa.isBlank()) {
            throw new IllegalArgumentException("Cancelamento exige justificativa.");
        }
        Pedido pedido = buscar(id);
        if (!StatusPedido.antesDaSaida(pedido.getStatus())) {
            throw new IllegalArgumentException("Não é possível cancelar depois da saída ou retirada.");
        }
        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setJustificativaCancelamento(justificativa);
        Pedido salvo = pedidoRepository.save(pedido);
        return salvo;
    }

}
