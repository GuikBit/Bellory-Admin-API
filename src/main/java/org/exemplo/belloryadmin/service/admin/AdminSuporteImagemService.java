package org.exemplo.belloryadmin.service.admin;

import lombok.RequiredArgsConstructor;
import org.exemplo.belloryadmin.model.dto.admin.SuporteImagemDTO;
import org.exemplo.belloryadmin.model.dto.admin.SuportePastaDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AdminSuporteImagemService {

    @Value("${file.upload.dir}")
    private String uploadDir;

    @Value("${file.upload.base-url}")
    private String baseUrl;

    private static final String SUPORTE_DIR = "admin/suporte";
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "svg");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    /**
     * Upload de imagem para a pasta de suporte
     */
    public SuporteImagemDTO uploadImagem(MultipartFile file, String pasta, String nomePersonalizado) {
        try {
            validarArquivo(file);

            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);

            String filename;
            if (nomePersonalizado != null && !nomePersonalizado.isBlank()) {
                String nomeSanitizado = sanitizeFilename(nomePersonalizado);
                // Remove extensão se o usuário incluiu uma
                if (nomeSanitizado.contains(".")) {
                    nomeSanitizado = nomeSanitizado.substring(0, nomeSanitizado.lastIndexOf("."));
                }
                filename = String.format("%s.%s", nomeSanitizado, extension);
            } else {
                filename = sanitizeFilename(originalFilename);
            }

            Path targetDirectory = resolveSuportePath(pasta);
            Files.createDirectories(targetDirectory);

            Path targetLocation = targetDirectory.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = buildRelativePath(pasta, filename);
            String fullUrl = baseUrl + "/" + relativePath;

            return SuporteImagemDTO.builder()
                    .nome(filename)
                    .url(fullUrl)
                    .relativePath(relativePath)
                    .pasta(pasta != null ? pasta : "")
                    .tamanho(file.getSize())
                    .build();
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao armazenar imagem: " + ex.getMessage(), ex);
        }
    }

    /**
     * Lista todas as imagens, opcionalmente filtradas por pasta
     */
    public List<SuporteImagemDTO> listarImagens(String pasta) {
        List<SuporteImagemDTO> imagens = new ArrayList<>();

        Path targetDirectory = resolveSuportePath(pasta);

        if (!Files.exists(targetDirectory)) {
            return imagens;
        }

        try (Stream<Path> paths = Files.list(targetDirectory)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> isImageFile(p.getFileName().toString()))
                    .forEach(p -> {
                        try {
                            String filename = p.getFileName().toString();
                            String relativePath = buildRelativePath(pasta, filename);
                            String fullUrl = baseUrl + "/" + relativePath;

                            imagens.add(SuporteImagemDTO.builder()
                                    .nome(filename)
                                    .url(fullUrl)
                                    .relativePath(relativePath)
                                    .pasta(pasta != null ? pasta : "")
                                    .tamanho(Files.size(p))
                                    .build());
                        } catch (IOException e) {
                            // ignora arquivo com erro
                        }
                    });
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao listar imagens: " + ex.getMessage(), ex);
        }

        return imagens;
    }

    /**
     * Deleta uma imagem pelo path relativo
     */
    public void deletarImagem(String relativePath) {
        try {
            if (relativePath == null || relativePath.isEmpty()) {
                throw new IllegalArgumentException("Path da imagem é obrigatório.");
            }

            // Validar que o path está dentro do diretório de suporte
            if (!relativePath.startsWith(SUPORTE_DIR)) {
                throw new IllegalArgumentException("Path inválido. Deve estar dentro do diretório de suporte.");
            }

            Path filePath = Paths.get(uploadDir, relativePath).normalize();

            // Segurança: garantir que o path normalizado ainda está dentro do uploadDir
            if (!filePath.startsWith(Paths.get(uploadDir).normalize())) {
                throw new IllegalArgumentException("Path inválido.");
            }

            if (!Files.exists(filePath)) {
                throw new IllegalArgumentException("Imagem não encontrada.");
            }

            Files.delete(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao deletar imagem: " + ex.getMessage(), ex);
        }
    }

    /**
     * Cria uma subpasta dentro do diretório de suporte
     */
    public SuportePastaDTO criarPasta(String nome) {
        try {
            if (nome == null || nome.isBlank()) {
                throw new IllegalArgumentException("Nome da pasta é obrigatório.");
            }

            String nomeSanitizado = sanitizeFolderName(nome);

            Path pastaPath = Paths.get(uploadDir, SUPORTE_DIR, nomeSanitizado);

            // Segurança: garantir que o path está dentro do diretório de suporte
            if (!pastaPath.normalize().startsWith(Paths.get(uploadDir, SUPORTE_DIR).normalize())) {
                throw new IllegalArgumentException("Nome de pasta inválido.");
            }

            if (Files.exists(pastaPath)) {
                throw new IllegalArgumentException("Pasta já existe: " + nomeSanitizado);
            }

            Files.createDirectories(pastaPath);

            return SuportePastaDTO.builder()
                    .nome(nomeSanitizado)
                    .caminho(SUPORTE_DIR + "/" + nomeSanitizado)
                    .totalImagens(0)
                    .build();
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao criar pasta: " + ex.getMessage(), ex);
        }
    }

    /**
     * Lista todas as pastas dentro do diretório de suporte
     */
    public List<SuportePastaDTO> listarPastas() {
        List<SuportePastaDTO> pastas = new ArrayList<>();

        Path suportePath = Paths.get(uploadDir, SUPORTE_DIR);

        if (!Files.exists(suportePath)) {
            return pastas;
        }

        try (Stream<Path> paths = Files.list(suportePath)) {
            paths.filter(Files::isDirectory)
                    .forEach(p -> {
                        String nome = p.getFileName().toString();
                        int totalImagens = contarImagens(p);

                        pastas.add(SuportePastaDTO.builder()
                                .nome(nome)
                                .caminho(SUPORTE_DIR + "/" + nome)
                                .totalImagens(totalImagens)
                                .build());
                    });
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao listar pastas: " + ex.getMessage(), ex);
        }

        return pastas;
    }

    /**
     * Deleta uma pasta e todas as imagens dentro dela
     */
    public void deletarPasta(String nome) {
        try {
            if (nome == null || nome.isBlank()) {
                throw new IllegalArgumentException("Nome da pasta é obrigatório.");
            }

            Path pastaPath = Paths.get(uploadDir, SUPORTE_DIR, nome).normalize();

            // Segurança: garantir que o path está dentro do diretório de suporte
            if (!pastaPath.startsWith(Paths.get(uploadDir, SUPORTE_DIR).normalize())) {
                throw new IllegalArgumentException("Nome de pasta inválido.");
            }

            if (!Files.exists(pastaPath) || !Files.isDirectory(pastaPath)) {
                throw new IllegalArgumentException("Pasta não encontrada: " + nome);
            }

            // Deletar todos os arquivos dentro da pasta
            try (Stream<Path> paths = Files.list(pastaPath)) {
                paths.forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        throw new RuntimeException("Erro ao deletar arquivo: " + p.getFileName(), e);
                    }
                });
            }

            // Deletar a pasta vazia
            Files.delete(pastaPath);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao deletar pasta: " + ex.getMessage(), ex);
        }
    }

    // ==================== Métodos auxiliares ====================

    private Path resolveSuportePath(String pasta) {
        if (pasta != null && !pasta.isBlank()) {
            String pastaSanitizada = sanitizeFolderName(pasta);
            return Paths.get(uploadDir, SUPORTE_DIR, pastaSanitizada);
        }
        return Paths.get(uploadDir, SUPORTE_DIR);
    }

    private String buildRelativePath(String pasta, String filename) {
        if (pasta != null && !pasta.isBlank()) {
            String pastaSanitizada = sanitizeFolderName(pasta);
            return SUPORTE_DIR + "/" + pastaSanitizada + "/" + filename;
        }
        return SUPORTE_DIR + "/" + filename;
    }

    private int contarImagens(Path directory) {
        try (Stream<Path> paths = Files.list(directory)) {
            return (int) paths.filter(Files::isRegularFile)
                    .filter(p -> isImageFile(p.getFileName().toString()))
                    .count();
        } catch (IOException e) {
            return 0;
        }
    }

    private boolean isImageFile(String filename) {
        String ext = getFileExtension(filename).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(ext);
    }

    private void validarArquivo(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Arquivo muito grande. Máximo: 5MB.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);

        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("Formato não permitido. Use: " + ALLOWED_EXTENSIONS);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "imagem.png";
        // Remove caracteres especiais, mantendo letras, números, pontos e hifens
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String sanitizeFolderName(String name) {
        if (name == null) return "";
        // Remove caracteres especiais, mantendo letras, números, hifens e underscores
        return name.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
    }
}
