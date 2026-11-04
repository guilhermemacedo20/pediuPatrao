package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.service.DashboardService;
import com.umc.pediupatrao.service.DashboardService.Filtro;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Controller
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("content", "dashboard :: content");
        return "layout";
    }

    @GetMapping("/api/dashboard/resumo")
    @ResponseBody
    public DashboardService.Resumo resumo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String status) {
        return dashboardService.resumo(dashboardService.criarFiltro(dataInicio, dataFim, status));
    }

    @GetMapping("/api/dashboard/vendas-por-dia")
    @ResponseBody
    public List<DashboardService.VendaDia> vendasPorDia(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String status) {
        return dashboardService.vendasPorDia(dashboardService.criarFiltro(dataInicio, dataFim, status));
    }

    @GetMapping("/api/dashboard/pedidos-por-status")
    @ResponseBody
    public List<DashboardService.PedidosStatus> pedidosPorStatus(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String status) {
        return dashboardService.pedidosPorStatus(dashboardService.criarFiltro(dataInicio, dataFim, status));
    }

    @GetMapping("/api/dashboard/produtos-mais-vendidos")
    @ResponseBody
    public List<DashboardService.ProdutoVendido> produtosMaisVendidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "5") int limite) {
        Filtro filtro = dashboardService.criarFiltro(dataInicio, dataFim, status);
        return dashboardService.produtosMaisVendidos(filtro, limite);
    }
}
