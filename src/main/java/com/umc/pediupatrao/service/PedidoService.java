package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.PedidoItem;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;

import org.springframework.beans.factory.annotation.Autowired;
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
        return pedidoRepository.save(pedido);
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    public Pedido atualizarStatus(String id, String status) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow();
        pedido.setStatus(status);
        return pedidoRepository.save(pedido);
    }

}
