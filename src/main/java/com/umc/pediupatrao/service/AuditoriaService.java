package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Auditoria;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class AuditoriaService {

    @Autowired
    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public void registrar(String operacao,
            String campo, String valorAnterior, String valorNovo) {

        Authentication autenticado = SecurityContextHolder.getContext().getAuthentication();

        Auditoria registro = new Auditoria();

        registro.setUsuario(autenticado.getName());
        registro.setDataHora(LocalDateTime.now());
        registro.setOperacao(operacao);
        registro.setCampo(campo);
        registro.setValorAnterior(valorAnterior);
        registro.setValorNovo(valorNovo);

        auditoriaRepository.save(registro);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public List<Auditoria> listar() {
        return auditoriaRepository.findAllByOrderByDataHoraDesc();
    }
}
