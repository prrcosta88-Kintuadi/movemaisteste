package br.com.desafio.orders.wallet;

import java.util.UUID;

import br.com.desafio.orders.wallet.dto.WalletResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unico endpoint ja implementado. Serve de referencia de estilo e e usado
 * pela bateria de aceite para conferir o saldo da carteira.
 */
@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/{walletId}")
    public WalletResponse getWallet(@PathVariable UUID walletId) {
        return walletService.findById(walletId);
    }
}
