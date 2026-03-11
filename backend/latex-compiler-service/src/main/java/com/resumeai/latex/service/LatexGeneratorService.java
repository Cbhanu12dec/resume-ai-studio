package com.resumeai.latex.service;

import com.resumeai.common.dto.ParsedResumeData;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LatexGeneratorService {

    private final Configuration freemarkerConfig;

    public String generate(ParsedResumeData data, String templateName) {
        String tmpl = (templateName != null && !templateName.isBlank()) ? templateName : "default";
        try {
            Template template = freemarkerConfig.getTemplate(tmpl + ".ftl");
            StringWriter writer = new StringWriter();
            template.process(Map.of("resume", data), writer);
            return writer.toString();
        } catch (Exception e) {
            log.error("LaTeX generation failed for template={}", tmpl, e);
            throw new RuntimeException("Failed to generate LaTeX", e);
        }
    }

    public byte[] compile(String latexSource) {
        java.nio.file.Path tempDir = null;
        try {
            // Use /tmp so Docker Desktop on macOS can mount the directory
            tempDir = java.nio.file.Files.createTempDirectory(
                    java.nio.file.Paths.get("/tmp"), "resumeai_latex_");
            java.nio.file.Path texFile = tempDir.resolve("resume.tex");
            java.nio.file.Files.writeString(texFile, latexSource);

            boolean usedDocker = false;
            ProcessBuilder pb = new ProcessBuilder(
                    "pdflatex", "-interaction=nonstopmode",
                    "-output-directory", tempDir.toString(),
                    texFile.toString());
            pb.directory(tempDir.toFile());
            pb.redirectErrorStream(true);

            Process process;
            try {
                process = pb.start();
            } catch (java.io.IOException e) {
                if (e.getMessage() != null && e.getMessage().contains("No such file")) {
                    log.warn("pdflatex not found locally — falling back to Docker (texlive/texlive)");
                    pb = new ProcessBuilder(
                            "docker", "run", "--rm",
                            "-v", tempDir.toAbsolutePath() + ":/workspace",
                            "-w", "/workspace",
                            "texlive/texlive",
                            "pdflatex", "-interaction=nonstopmode", "resume.tex");
                    pb.directory(tempDir.toFile());
                    pb.redirectErrorStream(true);
                    process = pb.start();
                    usedDocker = true;
                } else {
                    throw e;
                }
            }

            // Read output in background thread to prevent pipe-buffer deadlock
            final java.io.InputStream stdout = process.getInputStream();
            java.util.concurrent.Future<String> outputFuture =
                java.util.concurrent.Executors.newSingleThreadExecutor().submit(
                    () -> new String(stdout.readAllBytes()));

            int exitCode = process.waitFor();
            String output = "";
            try { output = outputFuture.get(10, java.util.concurrent.TimeUnit.SECONDS); }
            catch (Exception ignored) {}

            java.nio.file.Path pdfFile = tempDir.resolve("resume.pdf");
            if (exitCode == 0 && java.nio.file.Files.exists(pdfFile)) {
                byte[] pdf = java.nio.file.Files.readAllBytes(pdfFile);
                if (usedDocker) log.info("PDF compiled successfully via Docker");
                deleteDirectory(tempDir);
                return pdf;
            }
            log.error("pdflatex failed (exit={}, docker={}): {}", exitCode, usedDocker, output);
            throw new RuntimeException("PDF compilation failed (exit " + exitCode + "). Check logs for pdflatex output.");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to compile LaTeX to PDF: " + e.getMessage(), e);
        } finally {
            if (tempDir != null) {
                deleteDirectory(tempDir);
            }
        }
    }

    private void deleteDirectory(java.nio.file.Path dir) {
        try {
            java.nio.file.Files.walk(dir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> { try { java.nio.file.Files.delete(p); } catch (Exception ignored) {} });
        } catch (Exception ignored) {}
    }
}
