package br.gov.alvara.enums;

public enum StatusProcesso {
    RASCUNHO,
    AGUARDANDO_ANALISE,
    EM_ANALISE,
    PENDENTE,       // Documentos faltando ou erros na análise
    EXIGENCIA,      // Servidor emitiu exigências
    APROVADO,
    INDEFERIDO
}
