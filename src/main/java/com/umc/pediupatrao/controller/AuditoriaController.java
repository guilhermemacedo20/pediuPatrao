package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Auditoria;
import com.umc.pediupatrao.service.AuditoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
public class AuditoriaController {

    @Autowired
    private AuditoriaService auditoriaService;

    @GetMapping

    public List<Auditoria> listAudit() {
        return auditoriaService.listar();
    }

}
