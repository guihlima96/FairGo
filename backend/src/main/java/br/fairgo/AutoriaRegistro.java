package br.fairgo;

import java.time.Instant;

/** Identificação de quem registrou um evento e do instante atribuído pelo servidor. */
record AutoriaRegistro(String responsavel, Instant data) {}
