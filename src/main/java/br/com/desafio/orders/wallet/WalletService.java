package br.com.desafio.orders.wallet;

import java.util.UUID;

import br.com.desafio.orders.shared.Money;
import br.com.desafio.orders.shared.error.ApiException;
import br.com.desafio.orders.shared.error.ErrorCode;
import br.com.desafio.orders.wallet.dto.WalletResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exemplo de referencia: e assim que o starter espera que voce organize
 * controller -> service -> repository. Siga (ou melhore) esse padrao.
 */
@Service
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Transactional(readOnly = true)
    public WalletResponse findById(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ApiException(ErrorCode.WALLET_NOT_FOUND,
                        "Carteira " + walletId + " nao encontrada."));

        return new WalletResponse(
                wallet.getId().toString(),
                wallet.getOwnerName(),
                Money.format(wallet.getBalanceCents()));
    }
}
