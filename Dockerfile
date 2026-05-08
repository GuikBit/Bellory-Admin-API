# Imagem JRE leve
FROM eclipse-temurin:21-jre
WORKDIR /app

# Diretorio de uploads (suporte/imagens admin)
RUN mkdir -p /var/bellory-admin/dev/uploads /var/bellory-admin/prod/uploads && \
    chmod -R 755 /var/bellory-admin/dev/uploads /var/bellory-admin/prod/uploads

COPY app.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]
