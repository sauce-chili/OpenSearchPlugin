# Stage 1: Сборка плагина
FROM gradle:7.6.1-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle clean bundlePlugin --no-daemon

# Stage 2: Установка плагина в OpenSearch
FROM opensearchproject/opensearch:2.11.0

USER root
RUN yum install -y procps && yum clean all

# Копируем плагин
COPY --from=builder /app/build/distributions/stats-plugin.zip /tmp/
RUN /usr/share/opensearch/bin/opensearch-plugin install --batch file:///tmp/stats-plugin.zip && \
    rm -f /tmp/stats-plugin.zip && \
    chown -R opensearch:opensearch /usr/share/opensearch/plugins

USER opensearch