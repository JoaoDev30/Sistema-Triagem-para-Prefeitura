package br.gov.alvara.integration.s3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    /**
     * Faz upload do arquivo para o S3.
     * A chave é gerada com UUID para evitar colisão e sobrescrita.
     *
     * @return S3UploadResult com a key e URL pública (path-style)
     */
    public S3UploadResult upload(MultipartFile arquivo, String pasta) {
        String extensao = obterExtensao(arquivo.getOriginalFilename());
        String s3Key = pasta + "/" + UUID.randomUUID() + extensao;

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType(arquivo.getContentType())
                    .contentLength(arquivo.getSize())
                    // Objetos privados por padrão — acesso apenas via URL pré-assinada
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(arquivo.getInputStream(), arquivo.getSize()));
            log.info("Arquivo enviado ao S3: bucket={}, key={}", bucket, s3Key);

            String urlPermanente = "s3://" + bucket + "/" + s3Key;
            return new S3UploadResult(s3Key, urlPermanente, arquivo.getSize());

        } catch (IOException e) {
            log.error("Erro ao ler arquivo para upload: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao processar arquivo para upload", e);
        } catch (S3Exception e) {
            log.error("Erro ao enviar arquivo ao S3: {}", e.getMessage(), e);
            throw new RuntimeException("Falha no upload para o S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * Gera uma URL pré-assinada com validade de 60 minutos.
     * Usado para download seguro dos documentos.
     */
    public String gerarUrlPresignada(String s3Key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60))
                .getObjectRequest(r -> r.bucket(bucket).key(s3Key))
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        String url = presigned.url().toString();
        log.debug("URL pré-assinada gerada para key: {}", s3Key);
        return url;
    }

    /**
     * Remove um arquivo do S3.
     * Chamado quando um documento é deletado do sistema.
     */
    public void deletar(String s3Key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build());
            log.info("Arquivo removido do S3: key={}", s3Key);
        } catch (S3Exception e) {
            log.error("Erro ao deletar arquivo do S3: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao deletar arquivo do S3", e);
        }
    }

    private String obterExtensao(String nomeArquivo) {
        if (nomeArquivo == null || !nomeArquivo.contains(".")) return "";
        return nomeArquivo.substring(nomeArquivo.lastIndexOf("."));
    }

    // DTO interno para resultado do upload
    public record S3UploadResult(String s3Key, String urlS3, Long tamanhoBytes) {}
}
