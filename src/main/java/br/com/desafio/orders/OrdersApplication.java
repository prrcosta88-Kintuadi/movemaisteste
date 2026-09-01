package br.com.desafio.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ATENCAO: nao renomeie nem mova esta classe.
 * A bateria de aceite do avaliador carrega o contexto Spring a partir dela.
 */
@SpringBootApplication
public class OrdersApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrdersApplication.class, args);
    }
}
