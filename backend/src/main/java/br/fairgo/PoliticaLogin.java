package br.fairgo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("fairgo.login")
@Validated
record PoliticaLogin(@Valid @NotNull Cota rajada, @Valid @NotNull Cota par, @Valid @NotNull Cota conta,
                     @Valid @NotNull Cota origem, @Valid @NotNull Cota global) {
    record Cota(@Positive int maximo, @Positive int segundos) {}
}
