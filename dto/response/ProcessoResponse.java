package br.gov.alvara.dto.response;

import br.gov.alvara.enums.StatusProcesso;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProcessoResponse {
    private Long id;
    private String nomeEmpresa;
    private String cnpj;
    private String tipoEstabelecimento;
    private StatusProcesso status;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
    private String criadoPor;
    private String observacoes;
    private List<DocumentoResponse> documentos;
    private AnaliseIAResponse ultimaAnalise;
}
