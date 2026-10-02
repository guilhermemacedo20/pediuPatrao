package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    public Cliente novoCliente(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    // Método para excluir cliente
    public void excluir(String id) {
        auditoriaService.registrar("EXCLUSÃO DE CLIENTE",
                        "exclusão", id, "");
        clienteRepository.deleteById(id);
    }

    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    // Método para salvar um novo cliente ou atualizar um cliente existente
    public Cliente salvar(Cliente cliente) {
        // Se o cliente não tem ID (novo cliente), salva como novo
        if (cliente.getId() == null) {
            Cliente novoCliente = clienteRepository.save(cliente);
            auditoriaService.registrar("INCLUSÃO DE CLIENTE",
                    "cliente", null, novoCliente.getNome());
            return novoCliente;
        } // Se já tem ID (cliente existente), atualiza
        else {
            // Verifica se o cliente existe antes de atualizar
            Cliente anterior = clienteRepository.findById(cliente.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado para atualização."));

            Cliente atualizado = clienteRepository.save(cliente); // Atualiza o cliente existente

            if (!java.util.Objects.equals(anterior.getTelefone(), atualizado.getTelefone())) {
                auditoriaService.registrar("ALTERAÇÃO DE CLIENTE",
                        "telefone", anterior.getTelefone(), atualizado.getTelefone());
            }
            if (!java.util.Objects.equals(anterior.getLogradouro(), atualizado.getLogradouro())) {
                auditoriaService.registrar("ALTERAÇÃO DE CLIENTE",
                        "logradouro", anterior.getLogradouro(), atualizado.getLogradouro());
            }

            return atualizado; 

        }

    }

}
